import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export interface Spec extends TurboModule {
  getCount(): Promise<number>;
  setCount(count: number): Promise<boolean>;
  pinWidget(): Promise<boolean>;
  showFloatingBubble(): Promise<boolean>;
  hideFloatingBubble(): Promise<boolean>;
  checkOverlayPermission(): Promise<boolean>;
  requestOverlayPermission(): Promise<boolean>;
}

export default TurboModuleRegistry.get<Spec>('CounterBridge') as Spec | null;
