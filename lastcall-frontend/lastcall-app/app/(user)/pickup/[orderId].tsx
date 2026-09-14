import { useEffect, useState } from 'react';
import { View, Text, StyleSheet, ActivityIndicator, TouchableOpacity } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import QRCode from 'react-native-qrcode-svg';
import { getPickupCode } from '../../../api/order';
import { Colors } from '../../../constants/colors';

export default function PickupCodeScreen() {
  const router = useRouter();
  const { orderId } = useLocalSearchParams();
  const [order, setOrder] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getPickupCode(Number(orderId))
      .then(res => setOrder(res.data.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [orderId]);

  if (loading) {
    return <View style={styles.center}><ActivityIndicator color={Colors.primary} size="large" /></View>;
  }

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.back} onPress={() => router.replace('/(user)/profile')}>
        <Ionicons name="arrow-back" size={24} color={Colors.text} />
      </TouchableOpacity>

      <Text style={styles.title}>Pickup Code</Text>
      <Text style={styles.productName}>{order?.productName}</Text>

      <View style={styles.card}>
        <Text style={styles.codeLabel}>Your Code</Text>
        <Text style={styles.code}>{order?.pickupCode}</Text>
        {order?.qrCodeContent ? (
          <View style={styles.qrContainer}>
            <QRCode value={order.qrCodeContent} size={180} />
          </View>
        ) : null}
        <View style={[styles.statusBadge, { backgroundColor: Colors.paid }]}>
          <Text style={[styles.statusText, { color: Colors.paidText }]}>{order?.status}</Text>
        </View>
      </View>

      <Text style={styles.hint}>Show this code to the merchant when picking up your order.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, paddingHorizontal: 24, paddingTop: 60 },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: Colors.background },
  back: { marginBottom: 20 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text, marginBottom: 4 },
  productName: { fontSize: 15, color: Colors.textSecondary, marginBottom: 24 },
  card: { backgroundColor: Colors.card, borderRadius: 16, padding: 28, alignItems: 'center', shadowColor: '#000', shadowOffset: { width: 0, height: 2 }, shadowOpacity: 0.07, shadowRadius: 8, elevation: 3 },
  codeLabel: { fontSize: 13, color: Colors.textSecondary, marginBottom: 10 },
  code: { fontSize: 48, fontWeight: '700', color: Colors.text, letterSpacing: 8, marginBottom: 24 },
  qrContainer: { marginBottom: 24 },
  statusBadge: { paddingHorizontal: 16, paddingVertical: 6, borderRadius: 20 },
  statusText: { fontSize: 13, fontWeight: '600' },
  hint: { textAlign: 'center', color: Colors.textSecondary, fontSize: 13, marginTop: 24, lineHeight: 20 },
});
