import { useState, useEffect } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Alert, ActivityIndicator, ScrollView } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { createOrder } from '../../../api/order';
import { UserBrowseResponse } from '../../../api/product';
import { getReviewsByTemplate, ReviewResponse } from '../../../api/review';
import { addFavorite, removeFavorite, checkFavorite } from '../../../api/user';
import { Colors } from '../../../constants/colors';

export default function ListingDetailScreen() {
  const router = useRouter();
  const params = useLocalSearchParams();
  const item: UserBrowseResponse = JSON.parse(params.data as string);
  const [loading, setLoading] = useState(false);
  const [favorited, setFavorited] = useState(false);
  const [favLoading, setFavLoading] = useState(false);
  const [reviews, setReviews] = useState<ReviewResponse[]>([]);

  useEffect(() => {
    checkFavorite(item.listingId)
      .then(res => setFavorited(res.data.data))
      .catch(() => {});
    getReviewsByTemplate(item.templateId)
      .then(res => setReviews(res.data.data.content))
      .catch(() => {});
  }, []);

  const handleToggleFavorite = async () => {
    setFavLoading(true);
    try {
      if (favorited) {
        await removeFavorite(item.listingId);
        setFavorited(false);
      } else {
        await addFavorite(item.listingId);
        setFavorited(true);
      }
    } catch {
      Alert.alert('Error', 'Failed to update favourite');
    } finally {
      setFavLoading(false);
    }
  };

  const handleOrder = async () => {
    setLoading(true);
    try {
      const res = await createOrder(item.listingId);
      const order = res.data.data;
      router.push({ pathname: '/(user)/payment/[orderId]', params: { orderId: String(order.id), price: String(item.discountPrice) } });
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Failed to create order');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <TouchableOpacity onPress={() => router.back()}>
          <Ionicons name="arrow-back" size={24} color={Colors.text} />
        </TouchableOpacity>
        <TouchableOpacity onPress={handleToggleFavorite} disabled={favLoading} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
          <Ionicons name={favorited ? 'heart' : 'heart-outline'} size={26} color={favorited ? '#E05A5A' : Colors.textSecondary} />
        </TouchableOpacity>
      </View>

      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.productName}>{item.productName}</Text>
        <Text style={styles.merchantName}>{item.merchantName}</Text>
        <Text style={styles.address}>{item.merchantAddress}</Text>
        {item.description ? <Text style={styles.description}>{item.description}</Text> : null}

        <View style={styles.infoCard}>
          <Row label="Original Price" value={`$${Number(item.originalPrice).toFixed(2)}`} strike />
          <Row label="Discount Price" value={`$${Number(item.discountPrice).toFixed(2)}`} highlight />
          <Row label="Remaining" value={`${item.remainingQuantity} left`} />
          <Row label="Pickup Time" value={`${item.pickupStart} – ${item.pickupEnd}`} />
        </View>

        {reviews.length > 0 && (
          <View style={styles.reviewSection}>
            <Text style={styles.reviewTitle}>Reviews ({reviews.length})</Text>
            {reviews.map(r => (
              <View key={r.id} style={styles.reviewCard}>
                <View style={styles.reviewHeader}>
                  <View style={styles.stars}>
                    {[1,2,3,4,5].map(n => (
                      <Ionicons key={n} name={n <= r.rating ? 'star' : 'star-outline'} size={13} color={Colors.secondary} />
                    ))}
                  </View>
                  <Text style={styles.reviewDate}>{new Date(r.createdAt).toLocaleDateString()}</Text>
                </View>
                {r.content ? <Text style={styles.reviewContent}>{r.content}</Text> : null}
              </View>
            ))}
          </View>
        )}
      </ScrollView>

      <View style={styles.footer}>
        <View style={styles.priceBlock}>
          <Text style={styles.footerOriginal}>US${Number(item.originalPrice).toFixed(2)}</Text>
          <Text style={styles.footerDiscount}>US${Number(item.discountPrice).toFixed(2)}</Text>
        </View>
        <TouchableOpacity style={styles.reserveBtn} onPress={handleOrder} disabled={loading} activeOpacity={0.85}>
          {loading ? <ActivityIndicator color="#fff" size="small" /> : <Text style={styles.reserveText}>Reserve</Text>}
        </TouchableOpacity>
      </View>
    </View>
  );
}

function Row({ label, value, strike, highlight }: { label: string; value: string; strike?: boolean; highlight?: boolean }) {
  return (
    <View style={rowStyles.row}>
      <Text style={rowStyles.label}>{label}</Text>
      <Text style={[rowStyles.value, strike && rowStyles.strike, highlight && rowStyles.highlight]}>{value}</Text>
    </View>
  );
}

const rowStyles = StyleSheet.create({
  row: { flexDirection: 'row', justifyContent: 'space-between', paddingVertical: 10, borderBottomWidth: 1, borderBottomColor: Colors.border },
  label: { fontSize: 14, color: Colors.textSecondary },
  value: { fontSize: 14, fontWeight: '500', color: Colors.text },
  strike: { textDecorationLine: 'line-through', color: Colors.textMuted },
  highlight: { color: Colors.primary, fontWeight: '700', fontSize: 16 },
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingHorizontal: 20, paddingTop: 60, paddingBottom: 12 },
  content: { paddingHorizontal: 20, paddingBottom: 120 },
  productName: { fontSize: 24, fontWeight: '700', color: Colors.text, marginBottom: 4 },
  merchantName: { fontSize: 15, color: Colors.primary, marginBottom: 2 },
  address: { fontSize: 13, color: Colors.textSecondary, marginBottom: 16 },
  description: { fontSize: 14, color: Colors.textSecondary, marginBottom: 16, lineHeight: 20 },
  infoCard: { backgroundColor: Colors.card, borderRadius: 14, paddingHorizontal: 16, marginTop: 8, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2 },
  footer: {
    position: 'absolute', bottom: 0, left: 0, right: 0,
    backgroundColor: Colors.card,
    paddingHorizontal: 24, paddingVertical: 16,
    borderTopWidth: 1, borderTopColor: Colors.border,
    flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between',
  },
  priceBlock: { flexDirection: 'column' },
  footerOriginal: { fontSize: 13, color: Colors.textMuted, textDecorationLine: 'line-through' },
  footerDiscount: { fontSize: 22, fontWeight: '700', color: Colors.text },
  reserveBtn: {
    backgroundColor: Colors.primary,
    borderRadius: 28,
    paddingHorizontal: 36, paddingVertical: 14,
  },
  reserveText: { color: '#fff', fontSize: 16, fontWeight: '700' },
  reviewSection: { marginTop: 24 },
  reviewTitle: { fontSize: 16, fontWeight: '700', color: Colors.text, marginBottom: 12 },
  reviewCard: {
    backgroundColor: Colors.card, borderRadius: 12, padding: 14, marginBottom: 10,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.04, shadowRadius: 3, elevation: 1,
  },
  reviewHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 6 },
  stars: { flexDirection: 'row', gap: 2 },
  reviewDate: { fontSize: 12, color: Colors.textMuted },
  reviewContent: { fontSize: 14, color: Colors.textSecondary, lineHeight: 20 },
});
