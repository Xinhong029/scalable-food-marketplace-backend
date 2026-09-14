import { useEffect, useState } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet,
  Alert, TextInput, RefreshControl,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { getMerchantOrders, verifyPickupCode, OrderResponse } from '../../api/order';
import { Colors } from '../../constants/colors';
import { ORDER_STATUS_STYLES } from '../../constants/orderStatus';
import LoadingCenter from '../../components/LoadingCenter';
import EmptyState from '../../components/EmptyState';
import BottomModal from '../../components/BottomModal';

export default function MerchantOrdersScreen() {
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [verifyModal, setVerifyModal] = useState(false);
  const [code, setCode] = useState('');
  const [verifying, setVerifying] = useState(false);

  const load = async () => {
    try {
      const res = await getMerchantOrders();
      setOrders(res.data.data);
    } catch {
      // silent
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => { load(); }, []);

  const handleVerify = async () => {
    if (!code.trim()) return;
    setVerifying(true);
    try {
      const res = await verifyPickupCode({ pickupCode: code.trim() });
      const result = res.data.data;
      Alert.alert(
        result.success ? 'Verified' : 'Failed',
        result.success ? `${result.customerNickname} picked up ${result.productName}` : result.message,
        [{ text: 'OK', onPress: () => { setVerifyModal(false); setCode(''); load(); } }]
      );
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Verification failed');
    } finally {
      setVerifying(false);
    }
  };

  if (loading) return <LoadingCenter />;

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Orders</Text>
        <TouchableOpacity style={styles.verifyBtn} onPress={() => { setCode(''); setVerifyModal(true); }}>
          <Ionicons name="qr-code-outline" size={16} color="#fff" />
          <Text style={styles.verifyBtnText}>Verify</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={orders}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
        renderItem={({ item }) => {
          const s = ORDER_STATUS_STYLES[item.status] || ORDER_STATUS_STYLES.PENDING_PAYMENT;
          return (
            <View style={styles.card}>
              <View style={styles.cardTop}>
                <Text style={styles.productName}>{item.productName}</Text>
                <View style={[styles.badge, { backgroundColor: s.bg }]}>
                  <Text style={[styles.badgeText, { color: s.text }]}>{s.label}</Text>
                </View>
              </View>
              <Text style={styles.orderId}>Order #{item.id}</Text>
              <View style={styles.cardBottom}>
                <Text style={styles.price}>${Number(item.price).toFixed(2)}</Text>
                {item.status === 'PAID' && (
                  <TouchableOpacity style={styles.tapVerify} onPress={() => { setCode(''); setVerifyModal(true); }}>
                    <Text style={styles.tapVerifyText}>Tap to verify</Text>
                    <Ionicons name="arrow-forward" size={14} color={Colors.primary} />
                  </TouchableOpacity>
                )}
              </View>
            </View>
          );
        }}
        ListEmptyComponent={<EmptyState icon="bag-outline" text="No orders yet." />}
      />

      <BottomModal
        visible={verifyModal}
        title="Verify Pickup Code"
        onClose={() => setVerifyModal(false)}
        onConfirm={handleVerify}
        confirmLabel="Verify"
        confirming={verifying}
      >
        <Text style={styles.modalSubtitle}>Enter the customer's 6-digit code</Text>
        <TextInput
          style={styles.codeInput}
          value={code}
          onChangeText={setCode}
          placeholder="123456"
          placeholderTextColor={Colors.textMuted}
          keyboardType="number-pad"
          maxLength={6}
          textAlign="center"
        />
      </BottomModal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, paddingTop: 60 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, marginBottom: 16 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text },
  verifyBtn: { flexDirection: 'row', alignItems: 'center', gap: 6, paddingHorizontal: 14, paddingVertical: 8, borderRadius: 20, backgroundColor: Colors.primary },
  verifyBtnText: { color: '#fff', fontSize: 13, fontWeight: '600' },
  list: { paddingHorizontal: 20, paddingBottom: 20, gap: 12 },
  card: {
    backgroundColor: Colors.card, borderRadius: 14, padding: 16,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05, shadowRadius: 4, elevation: 2,
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 4 },
  productName: { fontSize: 15, fontWeight: '600', color: Colors.text, flex: 1, marginRight: 8 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20 },
  badgeText: { fontSize: 12, fontWeight: '600' },
  orderId: { fontSize: 12, color: Colors.textSecondary, marginBottom: 10 },
  cardBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  price: { fontSize: 16, fontWeight: '700', color: Colors.primary },
  tapVerify: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  tapVerifyText: { fontSize: 13, color: Colors.primary, fontWeight: '500' },
  modalSubtitle: { fontSize: 13, color: Colors.textSecondary, marginBottom: 20 },
  codeInput: {
    backgroundColor: Colors.background, borderRadius: 12,
    paddingVertical: 18, fontSize: 32, fontWeight: '700',
    color: Colors.text, letterSpacing: 12,
    borderWidth: 1, borderColor: Colors.border, marginBottom: 20,
  },
});
