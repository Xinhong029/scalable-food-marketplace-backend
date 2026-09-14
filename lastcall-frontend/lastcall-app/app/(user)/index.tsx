import { useEffect, useState, useCallback, useRef } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet,
  TextInput, RefreshControl, ActivityIndicator,
} from 'react-native';
import { useRouter, useFocusEffect } from 'expo-router';
import { Ionicons } from '@expo/vector-icons';
import { browseListings, UserBrowseResponse } from '../../api/product';
import { addFavorite, removeFavorite, checkFavorite } from '../../api/user';
import { Colors } from '../../constants/colors';
import LoadingCenter from '../../components/LoadingCenter';
import EmptyState from '../../components/EmptyState';

export default function DiscoverScreen() {
  const router = useRouter();
  const [listings, setListings] = useState<UserBrowseResponse[]>([]);
  const [filtered, setFiltered] = useState<UserBrowseResponse[]>([]);
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [favorites, setFavorites] = useState<Set<number>>(new Set());
  const [nextPage, setNextPage] = useState(1);
  const [hasNext, setHasNext] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const loadingMoreRef = useRef(false);

  const load = async () => {
    try {
      const res = await browseListings(0, 20);
      const page = res.data.data;
      const data = page.content;
      setListings(data);
      setFiltered(data);
      setNextPage(1);
      setHasNext(page.hasNext);
      const checks = await Promise.all(data.map(l => checkFavorite(l.listingId).then(r => r.data.data ? l.listingId : null).catch(() => null)));
      setFavorites(new Set(checks.filter((id): id is number => id !== null)));
    } catch {
      // silent
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  const loadMore = async () => {
    if (!hasNext || loadingMoreRef.current || search.trim()) return;
    loadingMoreRef.current = true;
    setLoadingMore(true);
    const requestedPage = nextPage;
    try {
      const res = await browseListings(requestedPage, 20);
      const page = res.data.data;
      const existingIds = new Set(listings.map(item => item.listingId));
      const newItems = page.content.filter(item => !existingIds.has(item.listingId));
      setListings(previous => [...previous, ...newItems]);
      const checks = await Promise.all(newItems.map(l => checkFavorite(l.listingId)
        .then(r => r.data.data ? l.listingId : null).catch(() => null)));
      setFavorites(previous => new Set([
        ...previous,
        ...checks.filter((id): id is number => id !== null),
      ]));
      setNextPage(requestedPage + 1);
      setHasNext(page.hasNext);
    } catch {
      // Keep the current page so scrolling can retry.
    } finally {
      loadingMoreRef.current = false;
      setLoadingMore(false);
    }
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  useEffect(() => {
    if (!search.trim()) {
      setFiltered(listings);
    } else {
      const q = search.toLowerCase();
      setFiltered(listings.filter(
        l => l.productName.toLowerCase().includes(q) || l.merchantName.toLowerCase().includes(q)
      ));
    }
  }, [search, listings]);

  const handleToggleFavorite = async (listingId: number) => {
    const isFav = favorites.has(listingId);
    setFavorites(prev => {
      const next = new Set(prev);
      isFav ? next.delete(listingId) : next.add(listingId);
      return next;
    });
    try {
      isFav ? await removeFavorite(listingId) : await addFavorite(listingId);
    } catch {
      setFavorites(prev => {
        const next = new Set(prev);
        isFav ? next.add(listingId) : next.delete(listingId);
        return next;
      });
    }
  };

  const renderItem = ({ item }: { item: UserBrowseResponse }) => {
    const soldOut = item.remainingQuantity <= 0;
    const isFav = favorites.has(item.listingId);

    return (
      <TouchableOpacity
        style={styles.card}
        activeOpacity={0.92}
        onPress={() => router.push({ pathname: '/(user)/listing/[id]', params: { id: String(item.listingId), data: JSON.stringify(item) } })}
      >
        {/* Image area with gradient overlay */}
        <View style={styles.imageContainer}>
          <View style={styles.imagePlaceholder}>
            <Ionicons name="restaurant" size={40} color={Colors.primaryLight} />
          </View>

          {/* Sold out badge */}
          {soldOut && (
            <View style={styles.soldOutBadge}>
              <Text style={styles.soldOutText}>Sold Out</Text>
            </View>
          )}

          {/* Quantity badge */}
          {!soldOut && (
            <View style={styles.qtyBadge}>
              <Text style={styles.qtyText}>{item.remainingQuantity} left</Text>
            </View>
          )}

          {/* Favourite button */}
          <TouchableOpacity
            onPress={(e) => { e.stopPropagation(); handleToggleFavorite(item.listingId); }}
            hitSlop={{ top: 8, bottom: 8, left: 8, right: 8 }}
            style={styles.heartBtn}
          >
            <Ionicons name={isFav ? 'heart' : 'heart-outline'} size={22} color={isFav ? '#E05A5A' : '#fff'} />
          </TouchableOpacity>
        </View>

        {/* Card content */}
        <View style={styles.cardContent}>
          <View style={styles.cardHeader}>
            <View style={styles.merchantIcon}>
              <Text style={styles.merchantIconText}>{item.merchantName[0]?.toUpperCase()}</Text>
            </View>
            <View style={styles.cardTitles}>
              <Text style={styles.productName} numberOfLines={1}>{item.productName}</Text>
              <Text style={styles.merchantName} numberOfLines={1}>{item.merchantName}</Text>
            </View>
          </View>

          {item.description ? (
            <Text style={styles.description} numberOfLines={2}>{item.description}</Text>
          ) : null}

          <View style={styles.bottomRow}>
            <Text style={styles.pickupTime}>Collect {item.pickupStart} – {item.pickupEnd}</Text>
            <View style={styles.priceCol}>
              <Text style={styles.originalPrice}>US${Number(item.originalPrice).toFixed(2)}</Text>
              <Text style={styles.discountPrice}>US${Number(item.discountPrice).toFixed(2)}</Text>
            </View>
          </View>
        </View>
      </TouchableOpacity>
    );
  };

  if (loading) return <LoadingCenter />;

  return (
    <View style={styles.container}>
      {/* Header */}
      <View style={styles.header}>
        <TextInput
          style={styles.search}
          placeholder="Search..."
          placeholderTextColor={Colors.textMuted}
          value={search}
          onChangeText={setSearch}
        />
        <Ionicons name="options-outline" size={22} color={Colors.textSecondary} style={styles.filterIcon} />
      </View>

      {/* Sort row */}
      <View style={styles.sortRow}>
        <Text style={styles.sortLabel}>Sort by: </Text>
        <Text style={styles.sortValue}>Relevance</Text>
        <Ionicons name="chevron-down" size={14} color={Colors.textSecondary} />
      </View>

      <FlatList
        data={filtered}
        keyExtractor={(item) => String(item.listingId)}
        renderItem={renderItem}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
        onEndReached={loadMore}
        onEndReachedThreshold={0.5}
        ListFooterComponent={loadingMore ? <ActivityIndicator color={Colors.primary} /> : null}
        ListEmptyComponent={<EmptyState icon="search-outline" text="No listings available right now." />}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },

  header: {
    flexDirection: 'row', alignItems: 'center',
    paddingHorizontal: 16, paddingTop: 56, paddingBottom: 10,
    gap: 10,
  },
  search: {
    flex: 1,
    backgroundColor: Colors.card,
    borderRadius: 24,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 14,
    color: Colors.text,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  filterIcon: { padding: 4 },

  sortRow: {
    flexDirection: 'row', alignItems: 'center',
    paddingHorizontal: 16, paddingBottom: 10,
  },
  sortLabel: { fontSize: 13, color: Colors.textSecondary },
  sortValue: { fontSize: 13, color: Colors.text, fontWeight: '600' },

  list: { paddingHorizontal: 16, paddingBottom: 24, gap: 16 },

  card: {
    backgroundColor: Colors.card,
    borderRadius: 16,
    overflow: 'hidden',
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.08,
    shadowRadius: 8,
    elevation: 3,
  },

  imageContainer: {
    height: 180,
    backgroundColor: '#F0EBE3',
    justifyContent: 'center',
    alignItems: 'center',
    position: 'relative',
  },
  imagePlaceholder: {
    width: '100%', height: '100%',
    justifyContent: 'center', alignItems: 'center',
    backgroundColor: '#F0EBE3',
  },

  soldOutBadge: {
    position: 'absolute', top: 12, left: 12,
    backgroundColor: 'rgba(0,0,0,0.55)',
    paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20,
  },
  soldOutText: { color: '#fff', fontSize: 12, fontWeight: '600' },

  qtyBadge: {
    position: 'absolute', bottom: 12, left: 12,
    backgroundColor: Colors.primary,
    paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20,
  },
  qtyText: { color: '#fff', fontSize: 12, fontWeight: '600' },

  cardContent: { padding: 10 },

  cardHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 4, gap: 10 },
  merchantIcon: {
    width: 40, height: 40, borderRadius: 8,
    backgroundColor: Colors.primaryLight,
    justifyContent: 'center', alignItems: 'center',
  },
  merchantIconText: { fontSize: 18, fontWeight: '700', color: Colors.primary },
  cardTitles: { flex: 1 },
  productName: { fontSize: 15, fontWeight: '700', color: Colors.text, marginBottom: 2 },
  merchantName: { fontSize: 12, color: Colors.textSecondary },

  description: { fontSize: 13, color: Colors.textSecondary, marginBottom: 4, lineHeight: 18 },

  bottomRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-end' },
  heartBtn: { position: 'absolute', top: 12, right: 12, backgroundColor: 'rgba(0,0,0,0.25)', borderRadius: 20, padding: 6 },
  pickupTime: { fontSize: 12, color: Colors.textSecondary, flex: 1 },
  priceCol: { alignItems: 'flex-end' },
  originalPrice: { fontSize: 12, color: Colors.textMuted, textDecorationLine: 'line-through' },
  discountPrice: { fontSize: 18, fontWeight: '700', color: Colors.primary },

});
