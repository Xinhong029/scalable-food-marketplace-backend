import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { Colors } from '../constants/colors';

export default function WelcomeScreen() {
  const router = useRouter();

  return (
    <View style={styles.container}>
      <View style={styles.hero}>
        <View style={styles.iconBox}>
          <Ionicons name="restaurant" size={32} color="#fff" />
        </View>
        <Text style={styles.title}>LastCall Eats</Text>
        <Text style={styles.subtitle}>Rescue food. Save money.</Text>
      </View>

      <View style={styles.cards}>
        <TouchableOpacity
          style={styles.card}
          onPress={() => router.push('/(auth)/user-login')}
          activeOpacity={0.8}
        >
          <View style={[styles.cardIcon, { backgroundColor: Colors.primaryLight }]}>
            <Ionicons name="person-outline" size={24} color={Colors.primary} />
          </View>
          <View>
            <Text style={styles.cardTitle}>I'm a Buyer</Text>
            <Text style={styles.cardDesc}>Browse & buy discounted food</Text>
          </View>
        </TouchableOpacity>

        <TouchableOpacity
          style={styles.card}
          onPress={() => router.push('/(auth)/merchant-login')}
          activeOpacity={0.8}
        >
          <View style={[styles.cardIcon, { backgroundColor: Colors.secondaryLight }]}>
            <Ionicons name="storefront-outline" size={24} color={Colors.secondary} />
          </View>
          <View>
            <Text style={styles.cardTitle}>I'm a Merchant</Text>
            <Text style={styles.cardDesc}>Sell surplus food at day's end</Text>
          </View>
        </TouchableOpacity>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1, backgroundColor: Colors.background,
    justifyContent: 'center', paddingHorizontal: 32,
  },
  hero: { alignItems: 'center', marginBottom: 56 },
  iconBox: {
    width: 72, height: 72, borderRadius: 18,
    backgroundColor: Colors.primary,
    justifyContent: 'center', alignItems: 'center', marginBottom: 16,
  },
  title: { fontSize: 28, fontWeight: '700', color: Colors.text, marginBottom: 6 },
  subtitle: { fontSize: 15, color: Colors.textSecondary },
  cards: { gap: 14 },
  card: {
    backgroundColor: Colors.card, borderRadius: 16,
    padding: 18, flexDirection: 'row', alignItems: 'center', gap: 16,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.06, shadowRadius: 4, elevation: 2,
  },
  cardIcon: {
    width: 48, height: 48, borderRadius: 12,
    justifyContent: 'center', alignItems: 'center',
  },
  cardTitle: { fontSize: 16, fontWeight: '600', color: Colors.text, marginBottom: 2 },
  cardDesc: { fontSize: 13, color: Colors.textSecondary },
});
