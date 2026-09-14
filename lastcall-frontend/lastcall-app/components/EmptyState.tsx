import { View, Text, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../constants/colors';

interface Props {
  icon: React.ComponentProps<typeof Ionicons>['name'];
  text: string;
  hint?: string;
}

export default function EmptyState({ icon, text, hint }: Props) {
  return (
    <View style={styles.empty}>
      <Ionicons name={icon} size={48} color={Colors.border} />
      <Text style={styles.text}>{text}</Text>
      {hint ? <Text style={styles.hint}>{hint}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  empty: { paddingTop: 80, alignItems: 'center', gap: 10 },
  text: { color: Colors.textSecondary, fontSize: 14 },
  hint: { color: Colors.textSecondary, fontSize: 13 },
});
