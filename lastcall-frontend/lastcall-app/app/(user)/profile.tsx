import { useState, useCallback } from 'react';
import {
  View, Text, StyleSheet, TouchableOpacity,
  FlatList, Alert, TextInput, RefreshControl,
} from 'react-native';
import { useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { getUserProfile, updateUserProfile, UserProfileResponse } from '../../api/user';
import { getUserOrders, OrderResponse } from '../../api/order';
import { createReview, getReviewByOrder } from '../../api/review';
import { useAuthStore } from '../../store/authStore';
import { Colors } from '../../constants/colors';
import { ORDER_STATUS_STYLES } from '../../constants/orderStatus';
import LoadingCenter from '../../components/LoadingCenter';
import EmptyState from '../../components/EmptyState';
import BottomModal from '../../components/BottomModal';

export default function MyScreen() {
  const router = useRouter();
  const { clearAuth } = useAuthStore();
  const [profile, setProfile] = useState<UserProfileResponse | null>(null);
  const [orders, setOrders] = useState<OrderResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [tab, setTab] = useState<'orders' | 'profile'>('orders');
  const [editModal, setEditModal] = useState(false);
  const [nickname, setNickname] = useState('');
  const [saving, setSaving] = useState(false);

  const [reviewModal, setReviewModal] = useState(false);
  const [reviewOrderId, setReviewOrderId] = useState<number | null>(null);
  const [reviewedOrderIds, setReviewedOrderIds] = useState<Set<number>>(new Set());
  const [rating, setRating] = useState(5);
  const [reviewContent, setReviewContent] = useState('');
  const [submittingReview, setSubmittingReview] = useState(false);

  const load = async () => {
    try {
      const [p, o] = await Promise.all([getUserProfile(), getUserOrders()]);
      setProfile(p.data.data);
      const orderList: OrderResponse[] = o.data.data;
      setOrders(orderList);
      const completed = orderList.filter(x => x.status === 'COMPLETED');
      const results = await Promise.all(completed.map(x => getReviewByOrder(x.id).catch(() => null)));
      const reviewed = new Set<number>();
      results.forEach((res, i) => {
        if (res?.data?.data) reviewed.add(completed[i].id);
      });
      setReviewedOrderIds(reviewed);
    } catch {
      // silent
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await updateUserProfile(nickname);
      setProfile(res.data.data);
      setEditModal(false);
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Failed to update');
    } finally {
      setSaving(false);
    }
  };

  const openReviewModal = (orderId: number) => {
    setReviewOrderId(orderId);
    setRating(5);
    setReviewContent('');
    setReviewModal(true);
  };

  const handleSubmitReview = async () => {
    if (!reviewOrderId) return;
    setSubmittingReview(true);
    try {
      await createReview({ orderId: reviewOrderId, rating, content: reviewContent });
      setReviewedOrderIds(prev => new Set(prev).add(reviewOrderId));
      setReviewModal(false);
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Failed to submit review');
    } finally {
      setSubmittingReview(false);
    }
  };

  const handleLogout = () => {
    Alert.alert('Logout', 'Are you sure?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Logout', style: 'destructive', onPress: () => clearAuth() },
    ]);
  };

  if (loading) return <LoadingCenter />;

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <Text style={styles.title}>My</Text>
        <TouchableOpacity onPress={handleLogout}>
          <Ionicons name="log-out-outline" size={24} color={Colors.textSecondary} />
        </TouchableOpacity>
      </View>

      {/* Tabs */}
      <View style={styles.tabs}>
        <TouchableOpacity style={[styles.tab, tab === 'orders' && styles.tabActive]} onPress={() => setTab('orders')}>
          <Text style={[styles.tabText, tab === 'orders' && styles.tabTextActive]}>Orders</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.tab, tab === 'profile' && styles.tabActive]} onPress={() => setTab('profile')}>
          <Text style={[styles.tabText, tab === 'profile' && styles.tabTextActive]}>Profile</Text>
        </TouchableOpacity>
      </View>

      {tab === 'orders' ? (
        <FlatList
          data={orders}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
          renderItem={({ item }) => {
            const s = ORDER_STATUS_STYLES[item.status] || ORDER_STATUS_STYLES.PENDING_PAYMENT;
            return (
              <TouchableOpacity
                style={styles.orderCard}
                activeOpacity={0.85}
                onPress={() => {
                  if (item.status === 'PENDING_PAYMENT') {
                    router.push({ pathname: '/(user)/payment/[orderId]', params: { orderId: String(item.id), price: String(item.price) } });
                  } else if (item.status === 'PAID') {
                    router.push({ pathname: '/(user)/pickup/[orderId]', params: { orderId: String(item.id) } });
                  }
                }}
              >
                <View style={styles.orderTop}>
                  <Text style={styles.orderProduct}>{item.productName}</Text>
                  <View style={[styles.badge, { backgroundColor: s.bg }]}>
                    <Text style={[styles.badgeText, { color: s.text }]}>{s.label}</Text>
                  </View>
                </View>
                <Text style={styles.orderId}>Order #{item.id}</Text>
                <View style={styles.orderBottom}>
                  <Text style={styles.orderPrice}>${Number(item.price).toFixed(2)}</Text>
                  <View style={styles.orderRight}>
                    {item.status === 'PENDING_PAYMENT' && <Text style={styles.payHint}>Tap to pay</Text>}
                    {item.status === 'PAID' && <Text style={styles.payHint}>Tap for pickup code</Text>}
                    {item.status === 'COMPLETED' && !reviewedOrderIds.has(item.id) && (
                      <TouchableOpacity onPress={() => openReviewModal(item.id)}>
                        <Text style={styles.reviewHint}>Write a review</Text>
                      </TouchableOpacity>
                    )}
                    {item.status === 'COMPLETED' && reviewedOrderIds.has(item.id) && (
                      <Text style={styles.reviewedHint}>Reviewed</Text>
                    )}
                    <Text style={styles.orderDate}>{new Date(item.createdAt).toLocaleDateString()}</Text>
                  </View>
                </View>
              </TouchableOpacity>
            );
          }}
          ListEmptyComponent={<EmptyState icon="bag-outline" text="No orders yet." />}
        />
      ) : (
        <View style={styles.profileSection}>
          <View style={styles.avatarCircle}>
            <Text style={styles.avatarText}>{profile?.nickname?.[0]?.toUpperCase() || '?'}</Text>
          </View>
          <Text style={styles.nicknameText}>{profile?.nickname}</Text>
          <Text style={styles.emailText}>{profile?.email}</Text>
          <TouchableOpacity
            style={styles.editBtn}
            onPress={() => { setNickname(profile?.nickname || ''); setEditModal(true); }}
          >
            <Text style={styles.editBtnText}>Edit Profile</Text>
          </TouchableOpacity>
        </View>
      )}

      <BottomModal
        visible={editModal}
        title="Edit Nickname"
        onClose={() => setEditModal(false)}
        onConfirm={handleSave}
        confirming={saving}
      >
        <TextInput
          style={styles.input}
          value={nickname}
          onChangeText={setNickname}
          placeholder="Nickname"
          placeholderTextColor={Colors.textMuted}
        />
      </BottomModal>

      <BottomModal
        visible={reviewModal}
        title="Write a Review"
        confirmLabel="Submit"
        onClose={() => setReviewModal(false)}
        onConfirm={handleSubmitReview}
        confirming={submittingReview}
      >
        <View style={styles.stars}>
          {[1, 2, 3, 4, 5].map(n => (
            <TouchableOpacity key={n} onPress={() => setRating(n)}>
              <Ionicons name={n <= rating ? 'star' : 'star-outline'} size={32} color={Colors.secondary} />
            </TouchableOpacity>
          ))}
        </View>
        <TextInput
          style={[styles.input, styles.reviewInput]}
          value={reviewContent}
          onChangeText={setReviewContent}
          placeholder="Share your experience (optional)"
          placeholderTextColor={Colors.textMuted}
          multiline
        />
      </BottomModal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, paddingTop: 60 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, marginBottom: 16 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text },
  tabs: { flexDirection: 'row', paddingHorizontal: 20, marginBottom: 16, gap: 8 },
  tab: { paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20, backgroundColor: Colors.border },
  tabActive: { backgroundColor: Colors.primary },
  tabText: { fontSize: 13, color: Colors.textSecondary, fontWeight: '500' },
  tabTextActive: { color: '#fff' },

  // Orders list
  list: { paddingHorizontal: 20, paddingBottom: 20, gap: 12 },
  orderCard: {
    backgroundColor: Colors.card, borderRadius: 14, padding: 16,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05, shadowRadius: 4, elevation: 2,
  },
  orderTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 4 },
  orderProduct: { fontSize: 15, fontWeight: '600', color: Colors.text, flex: 1, marginRight: 8 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20 },
  badgeText: { fontSize: 12, fontWeight: '600' },
  orderId: { fontSize: 12, color: Colors.textSecondary, marginBottom: 10 },
  orderBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  orderPrice: { fontSize: 16, fontWeight: '700', color: Colors.primary },
  orderRight: { alignItems: 'flex-end', gap: 2 },
  payHint: { fontSize: 12, color: Colors.primary, fontWeight: '600' },
  reviewHint: { fontSize: 12, color: Colors.secondary, fontWeight: '600' },
  reviewedHint: { fontSize: 12, color: Colors.textMuted },
  orderDate: { fontSize: 12, color: Colors.textSecondary },

  // Profile section
  profileSection: { alignItems: 'center', paddingTop: 32 },
  avatarCircle: {
    width: 80, height: 80, borderRadius: 40,
    backgroundColor: Colors.primaryLight,
    justifyContent: 'center', alignItems: 'center', marginBottom: 12,
  },
  avatarText: { fontSize: 32, fontWeight: '700', color: Colors.primary },
  nicknameText: { fontSize: 20, fontWeight: '700', color: Colors.text, marginBottom: 4 },
  emailText: { fontSize: 14, color: Colors.textSecondary, marginBottom: 20 },
  editBtn: { paddingHorizontal: 24, paddingVertical: 10, borderRadius: 20, backgroundColor: Colors.primaryLight },
  editBtnText: { color: Colors.primary, fontWeight: '600', fontSize: 14 },

  // Modal input
  input: {
    backgroundColor: Colors.background, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 14,
    fontSize: 15, color: Colors.text,
    borderWidth: 1, borderColor: Colors.border, marginBottom: 16,
  },
  stars: { flexDirection: 'row', justifyContent: 'center', gap: 8, marginBottom: 16 },
  reviewInput: { height: 100, textAlignVertical: 'top' },
});
