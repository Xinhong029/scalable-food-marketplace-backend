import { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { createPaymentIntent } from '../../../api/payment';
import { Colors } from '../../../constants/colors';

export default function PaymentScreen() {
  const router = useRouter();
  const { orderId, price } = useLocalSearchParams();
  const [paymentMethodId, setPaymentMethodId] = useState('');
  const [loading, setLoading] = useState(false);

  const handlePay = async () => {
    if (!paymentMethodId.trim()) {
      Alert.alert('Error', 'Please enter a payment method ID');
      return;
    }
    setLoading(true);
    try {
      await createPaymentIntent({ orderId: Number(orderId), paymentMethodId: paymentMethodId.trim(), paymentType: 'STRIPE' });
      router.replace({ pathname: '/(user)/pickup/[orderId]', params: { orderId: String(orderId) } });
    } catch (e: any) {
      Alert.alert('Payment failed', e.response?.data?.message || 'Please try again');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.back} onPress={() => router.back()}>
        <Ionicons name="arrow-back" size={24} color={Colors.text} />
      </TouchableOpacity>

      <Text style={styles.title}>Payment</Text>
      <Text style={styles.subtitle}>Complete your order</Text>

      <View style={styles.amountCard}>
        <Text style={styles.amountLabel}>Amount to pay</Text>
        <Text style={styles.amount}>${Number(price).toFixed(2)}</Text>
      </View>

      <View style={styles.form}>
        <Text style={styles.label}>Payment Method ID</Text>
        <TextInput
          style={styles.input}
          placeholder="pm_card_visa"
          placeholderTextColor={Colors.textMuted}
          value={paymentMethodId}
          onChangeText={setPaymentMethodId}
          autoCapitalize="none"
        />
        <Text style={styles.hint}>Use Stripe test payment method IDs for testing.</Text>
      </View>

      <TouchableOpacity style={styles.button} onPress={handlePay} disabled={loading} activeOpacity={0.85}>
        {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Pay ${Number(price).toFixed(2)}</Text>}
      </TouchableOpacity>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, paddingHorizontal: 24, paddingTop: 60 },
  back: { marginBottom: 24 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text, marginBottom: 4 },
  subtitle: { fontSize: 14, color: Colors.textSecondary, marginBottom: 24 },
  amountCard: { backgroundColor: Colors.card, borderRadius: 14, padding: 20, alignItems: 'center', marginBottom: 28, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  amountLabel: { fontSize: 13, color: Colors.textSecondary, marginBottom: 6 },
  amount: { fontSize: 36, fontWeight: '700', color: Colors.primary },
  form: { marginBottom: 24 },
  label: { fontSize: 13, color: Colors.text, marginBottom: 8 },
  input: { backgroundColor: Colors.card, borderRadius: 12, paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, color: Colors.text, borderWidth: 1, borderColor: Colors.border, marginBottom: 8 },
  hint: { fontSize: 12, color: Colors.textMuted },
  button: { backgroundColor: Colors.primary, borderRadius: 12, paddingVertical: 16, alignItems: 'center' },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
});
