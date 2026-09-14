import { useState, useCallback } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet, RefreshControl } from 'react-native';
import { useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { getFavorites, removeFavorite, FavoriteListingResponse } from '../../api/user';
import { Colors } from '../../constants/colors';
import LoadingCenter from '../../components/LoadingCenter';
import EmptyState from '../../components/EmptyState';

export default function FavoritesScreen() {
  const [favorites, setFavorites] = useState<FavoriteListingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = async () => {
    try {
      const res = await getFavorites();
      setFavorites(res.data.data);
    } catch {
      // silent
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  const handleUnfavorite = async (listingId: number) => {
    await removeFavorite(listingId);
    setFavorites(f => f.filter(x => x.listingId !== listingId));
  };

  if (loading) return <LoadingCenter />;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Favourites</Text>
      <FlatList
        data={favorites}
        keyExtractor={(item) => String(item.listingId)}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <View style={styles.iconBox}>
              <Text style={styles.iconText}>{item.merchantName[0]?.toUpperCase()}</Text>
            </View>
            <View style={styles.info}>
              <Text style={styles.productName}>{item.productName}</Text>
              <Text style={styles.merchantName}>{item.merchantName}</Text>
              <View style={styles.row}>
                <Text style={styles.price}>US${Number(item.discountPrice).toFixed(2)}</Text>
                <Text style={styles.dot}>·</Text>
                <Text style={styles.pickup}>{item.pickupStart} – {item.pickupEnd}</Text>
              </View>
            </View>
            <TouchableOpacity onPress={() => handleUnfavorite(item.listingId)} hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}>
              <Ionicons name="heart" size={22} color="#E05A5A" />
            </TouchableOpacity>
          </View>
        )}
        ListEmptyComponent={<EmptyState icon="heart-outline" text="No favourites yet." hint="Tap the heart on any listing to save it." />}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, paddingTop: 60 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text, paddingHorizontal: 20, marginBottom: 16 },
  list: { paddingHorizontal: 20, paddingBottom: 20, gap: 12 },
  card: {
    backgroundColor: Colors.card, borderRadius: 14,
    padding: 14, flexDirection: 'row', alignItems: 'center', gap: 12,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05, shadowRadius: 4, elevation: 2,
  },
  iconBox: {
    width: 44, height: 44, borderRadius: 10,
    backgroundColor: Colors.primaryLight,
    justifyContent: 'center', alignItems: 'center',
  },
  iconText: { fontSize: 20, fontWeight: '700', color: Colors.primary },
  info: { flex: 1 },
  productName: { fontSize: 15, fontWeight: '600', color: Colors.text, marginBottom: 2 },
  merchantName: { fontSize: 12, color: Colors.textSecondary, marginBottom: 4 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 4 },
  price: { fontSize: 13, fontWeight: '700', color: Colors.primary },
  dot: { fontSize: 13, color: Colors.textMuted },
  pickup: { fontSize: 12, color: Colors.textSecondary },
});
