/**
 * Clean White Screen UI with Interactive Counter Box
 *
 * @format
 */

import React, { useEffect, useState, useCallback } from 'react';
import {
  AppState,
  DeviceEventEmitter,
  NativeModules,
  StatusBar,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
} from 'react-native';
import {
  SafeAreaProvider,
  useSafeAreaInsets,
} from 'react-native-safe-area-context';
import NativeCounterBridge from './src/specs/NativeCounterBridge';
import { TurboModuleRegistry } from 'react-native';

const getCounterBridge = () => {
  return (
    NativeModules.CounterBridge ||
    TurboModuleRegistry.get('CounterBridge') ||
    NativeCounterBridge
  );
};

function AppContent() {
  const insets = useSafeAreaInsets();
  const [count, setCount] = useState<number>(0);

  // Sync count from widget / native shared preferences
  const fetchNativeCount = useCallback(async () => {
    const bridge = getCounterBridge();
    if (bridge?.getCount) {
      try {
        const val = await bridge.getCount();
        if (typeof val === 'number') {
          setCount(val);
        }
      } catch (e) {
        console.warn('[TurboModule] Failed to get count:', e);
      }
    }
  }, []);

  // Update native widget state whenever count changes from user interaction
  const updateNativeCount = (newCount: number) => {
    setCount(newCount);
    const bridge = getCounterBridge();
    if (bridge?.setCount) {
      bridge.setCount(newCount)
        .catch((e: any) =>
          console.warn('[TurboModule] Failed to set count:', e)
        );
    }
  };

  useEffect(() => {
    fetchNativeCount();

    // Listen to real-time count change events from TileService, Widget, or Background
    const countEventSubscription = DeviceEventEmitter.addListener(
      'onCountChanged',
      (data: { count?: number }) => {
        console.log('[RealTime Sync] onCountChanged received:', data?.count);
        if (data && typeof data.count === 'number') {
          setCount(data.count);
        }
      }
    );

    const appStateSubscription = AppState.addEventListener('change', nextAppState => {
      if (nextAppState === 'active') {
        fetchNativeCount();
      }
    });

    return () => {
      countEventSubscription.remove();
      appStateSubscription.remove();
    };
  }, [fetchNativeCount]);

  const handleIncrement = () => {
    const nextVal = count + 1;
    updateNativeCount(nextVal);
  };

  const handleDecrement = () => {
    const nextVal = count > 0 ? count - 1 : 0;
    updateNativeCount(nextVal);
  };

  const handleReset = () => {
    updateNativeCount(0);
  };

  return (
    <View
      style={[
        styles.container,
        {
          paddingTop: insets.top,
          paddingBottom: insets.bottom,
          paddingLeft: insets.left,
          paddingRight: insets.right,
        },
      ]}
    >
      <View style={styles.centerContainer}>
        {/* Main Counter Box Card */}
        <View style={styles.cardBox}>
          <Text style={styles.boxTitle}>COUNTER BOX</Text>

          {/* Interactive Row: [-] Left | Count Center | [+] Right */}
          <View style={styles.counterRow}>
            {/* Decrease Button (Left) */}
            <TouchableOpacity
              activeOpacity={0.7}
              style={[styles.actionBtn, styles.decreaseBtn]}
              onPress={handleDecrement}
            >
              <Text style={styles.btnText}>−</Text>
            </TouchableOpacity>

            {/* Center Count Value */}
            <View style={styles.countBadge}>
              <Text style={styles.countText}>{count}</Text>
            </View>

            {/* Increase Button (Right) */}
            <TouchableOpacity
              activeOpacity={0.7}
              style={[styles.actionBtn, styles.increaseBtn]}
              onPress={handleIncrement}
            >
              <Text style={styles.btnText}>+</Text>
            </TouchableOpacity>
          </View>

          {/* Reset Action */}
          <TouchableOpacity
            activeOpacity={0.6}
            style={styles.resetBtn}
            onPress={handleReset}
          >
            <Text style={styles.resetBtnText}>Reset Counter</Text>
          </TouchableOpacity>

          {/* Add Widget to Home Screen */}
          <TouchableOpacity
            activeOpacity={0.7}
            style={styles.pinBtn}
            onPress={() => {
              const bridge = getCounterBridge();
              if (bridge?.pinWidget) {
                bridge.pinWidget();
              }
            }}
          >
            <Text style={styles.pinBtnText}>+ Add Widget to Home Screen</Text>
          </TouchableOpacity>

        </View>
      </View>
    </View>
  );
}

function App() {
  return (
    <SafeAreaProvider>
      <StatusBar barStyle="dark-content" />
      <AppContent />
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#FFFFFF',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 24,
  },
  cardBox: {
    width: '100%',
    maxWidth: 340,
    backgroundColor: '#FFFFFF',
    borderRadius: 20,
    paddingVertical: 28,
    paddingHorizontal: 20,
    alignItems: 'center',
    borderWidth: 1.5,
    borderColor: '#E2E8F0',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: 8 },
    shadowOpacity: 0.08,
    shadowRadius: 16,
    elevation: 4,
  },
  boxTitle: {
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 1.5,
    color: '#64748B',
    marginBottom: 24,
  },
  counterRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    marginBottom: 20,
  },
  actionBtn: {
    width: 60,
    height: 60,
    borderRadius: 30,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.12,
    shadowRadius: 6,
    elevation: 3,
  },
  decreaseBtn: {
    backgroundColor: '#EF4444',
  },
  increaseBtn: {
    backgroundColor: '#10B981',
  },
  btnText: {
    fontSize: 28,
    fontWeight: '700',
    color: '#FFFFFF',
    lineHeight: 32,
  },
  countBadge: {
    minWidth: 100,
    paddingHorizontal: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  countText: {
    fontSize: 48,
    fontWeight: '800',
    color: '#0F172A',
  },
  resetBtn: {
    paddingVertical: 8,
    paddingHorizontal: 16,
    borderRadius: 8,
    backgroundColor: '#F1F5F9',
  },
  resetBtnText: {
    fontSize: 13,
    fontWeight: '600',
    color: '#64748B',
  },
  pinBtn: {
    marginTop: 14,
    paddingVertical: 10,
    paddingHorizontal: 16,
    borderRadius: 10,
    backgroundColor: '#EEF2F6',
    borderWidth: 1,
    borderColor: '#E2E8F0',
  },
  pinBtnText: {
    fontSize: 12,
    fontWeight: '600',
    color: '#3B82F6',
  },
});

export default App;


