import { useState, useCallback } from 'react';
import { View, Text, StyleSheet, RefreshControl, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { useFocusEffect } from 'expo-router';
import { getMerchantDashboard, getMerchantProfile, MerchantDashboardResponse } from '../../api/merchant';
import { Colors } from '../../constants/colors';
import LoadingCenter from '../../components/LoadingCenter';

export default function DashboardScreen() {
  const [dashboard, setDashboard] = useState<MerchantDashboardResponse | null>(null);
  const [merchantName, setMerchantName] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const load = async () => {
    try {
      const [d, p] = await Promise.all([getMerchantDashboard(), getMerchantProfile()]);
      setDashboard(d.data.data);
      setMerchantName(p.data.data.name);
    } catch {
      // silent
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useFocusEffect(useCallback(() => { load(); }, []));

  if (loading) return <LoadingCenter />;

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
    >
      <Text style={styles.title}>Dashboard</Text>
      <Text style={styles.subtitle}>{merchantName}</Text>

      <StatCard iconName="bag-outline" iconBg={Colors.primaryLight} iconColor={Colors.primary} label="Today's Orders" value={String(dashboard?.todayOrderCount ?? 0)} />
      <StatCard iconName="cash-outline" iconBg={Colors.secondaryLight} iconColor={Colors.secondary} label="Today's Revenue" value={`$${Number(dashboard?.todayRevenue ?? 0).toFixed(2)}`} />
      <StatCard iconName="cube-outline" iconBg="#FFF8E6" iconColor="#C27C00" label="Active Listings" value={String(dashboard?.activeListingCount ?? 0)} />
    </ScrollView>
  );
}

function StatCard({ iconName, iconBg, iconColor, label, value }: { iconName: any; iconBg: string; iconColor: string; label: string; value: string }) {
  return (
    <View style={styles.card}>
      <View style={[styles.cardIcon, { backgroundColor: iconBg }]}>
        <Ionicons name={iconName} size={24} color={iconColor} />
      </View>
      <View>
        <Text style={styles.cardLabel}>{label}</Text>
        <Text style={styles.cardValue}>{value}</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  content: { paddingTop: 60, paddingHorizontal: 20, paddingBottom: 20, gap: 12 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text, marginBottom: 2 },
  subtitle: { fontSize: 14, color: Colors.textSecondary, marginBottom: 20 },
  card: {
    backgroundColor: Colors.card, borderRadius: 14, padding: 18,
    flexDirection: 'row', alignItems: 'center', gap: 16,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05, shadowRadius: 4, elevation: 2,
  },
  cardIcon: { width: 48, height: 48, borderRadius: 12, justifyContent: 'center', alignItems: 'center' },
  cardLabel: { fontSize: 13, color: Colors.textSecondary, marginBottom: 2 },
  cardValue: { fontSize: 24, fontWeight: '700', color: Colors.text },
});
