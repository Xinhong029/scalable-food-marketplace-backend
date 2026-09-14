import { useEffect, useState } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet,
  Alert, Modal, TextInput, RefreshControl, ScrollView,
  KeyboardAvoidingView, Platform, ActivityIndicator,
} from 'react-native';
import { useForm, Controller } from 'react-hook-form';
import { Ionicons } from '@expo/vector-icons';
import {
  getTemplates, createTemplate, updateTemplate, deleteTemplate,
  getMerchantListings, createListing, deleteListing,
  TemplateResponse, ListingResponse,
} from '../../api/product';
import { Colors } from '../../constants/colors';
import LoadingCenter from '../../components/LoadingCenter';
import EmptyState from '../../components/EmptyState';

export default function ProductsScreen() {
  const [tab, setTab] = useState<'templates' | 'listings'>('templates');
  const [templates, setTemplates] = useState<TemplateResponse[]>([]);
  const [listings, setListings] = useState<ListingResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  // Template modal
  const [templateModal, setTemplateModal] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<TemplateResponse | null>(null);
  const { control: tc, handleSubmit: ths, reset: tr, formState: { isSubmitting: tis } } = useForm<{ name: string; description: string; originalPrice: string }>();

  // Listing modal — templateId stored as number in state, not form
  const [listingModal, setListingModal] = useState(false);
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const { control: lc, handleSubmit: lhs, reset: lr, formState: { isSubmitting: lis } } = useForm<{ discountPrice: string; quantity: string; pickupStart: string; pickupEnd: string; date: string }>();

  const load = async () => {
    try {
      const [t, l] = await Promise.all([getTemplates(), getMerchantListings()]);
      setTemplates(t.data.data);
      setListings(l.data.data);
    } catch {
      // silent
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => { load(); }, []);

  const openNewTemplate = () => {
    setEditingTemplate(null);
    tr({ name: '', description: '', originalPrice: '' });
    setTemplateModal(true);
  };

  const openEditTemplate = (t: TemplateResponse) => {
    setEditingTemplate(t);
    tr({ name: t.name, description: t.description || '', originalPrice: String(t.originalPrice) });
    setTemplateModal(true);
  };

  const onSaveTemplate = ths(async (data) => {
    try {
      const payload = { name: data.name, description: data.description, originalPrice: Number(data.originalPrice) };
      if (editingTemplate) await updateTemplate(editingTemplate.id, payload);
      else await createTemplate(payload);
      setTemplateModal(false);
      load();
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Failed to save template');
    }
  });

  const onDeleteTemplate = (id: number) => {
    Alert.alert('Delete template', 'Are you sure?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Delete', style: 'destructive', onPress: async () => { await deleteTemplate(id); load(); } },
    ]);
  };

  const openNewListing = () => {
    setSelectedTemplateId(templates.length > 0 ? templates[0].id : null);
    lr({ discountPrice: '', quantity: '', pickupStart: '', pickupEnd: '', date: new Date().toISOString().split('T')[0] });
    setListingModal(true);
  };

  const onSaveListing = lhs(async (data) => {
    if (!selectedTemplateId) {
      Alert.alert('Error', 'Please select a template');
      return;
    }
    try {
      await createListing({
        templateId: selectedTemplateId,
        discountPrice: Number(data.discountPrice),
        quantity: Number(data.quantity),
        pickupStart: data.pickupStart,
        pickupEnd: data.pickupEnd,
        date: data.date,
      });
      setListingModal(false);
      load();
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Failed to create listing');
    }
  });

  const onDeleteListing = (id: number) => {
    Alert.alert('Remove listing', 'Take this off the shelf?', [
      { text: 'Cancel', style: 'cancel' },
      { text: 'Remove', style: 'destructive', onPress: async () => { await deleteListing(id); load(); } },
    ]);
  };

  if (loading) return <LoadingCenter />;

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Products</Text>

      <View style={styles.tabs}>
        <TouchableOpacity style={[styles.tab, tab === 'templates' && styles.tabActive]} onPress={() => setTab('templates')}>
          <Text style={[styles.tabText, tab === 'templates' && styles.tabTextActive]}>Templates</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.tab, tab === 'listings' && styles.tabActive]} onPress={() => setTab('listings')}>
          <Text style={[styles.tabText, tab === 'listings' && styles.tabTextActive]}>Today's Listings</Text>
        </TouchableOpacity>
      </View>

      {tab === 'templates' ? (
        <FlatList
          data={templates}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.cardTop}>
                <Text style={styles.cardName}>{item.name}</Text>
                <View style={[styles.badge, { backgroundColor: item.isActive ? Colors.completed : Colors.border }]}>
                  <Text style={[styles.badgeText, { color: item.isActive ? Colors.completedText : Colors.textSecondary }]}>
                    {item.isActive ? 'Active' : 'Inactive'}
                  </Text>
                </View>
              </View>
              {item.description ? <Text style={styles.cardDesc}>{item.description}</Text> : null}
              <View style={styles.cardBottom}>
                <Text style={styles.price}>${Number(item.originalPrice).toFixed(2)}</Text>
                <View style={styles.actions}>
                  <TouchableOpacity style={styles.actionBtn} onPress={() => openEditTemplate(item)}>
                    <Ionicons name="create-outline" size={20} color={Colors.textSecondary} />
                  </TouchableOpacity>
                  <TouchableOpacity style={styles.actionBtn} onPress={() => onDeleteTemplate(item.id)}>
                    <Ionicons name="trash-outline" size={20} color={Colors.textSecondary} />
                  </TouchableOpacity>
                </View>
              </View>
            </View>
          )}
          ListEmptyComponent={<EmptyState icon="cube-outline" text="No templates yet." />}
        />
      ) : (
        <FlatList
          data={listings}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
          renderItem={({ item }) => (
            <View style={styles.card}>
              <View style={styles.cardTop}>
                <Text style={styles.cardName}>{item.templateName}</Text>
                <View style={[styles.badge, { backgroundColor: item.isAvailable && item.remainingQuantity > 0 ? Colors.completed : Colors.border }]}>
                  <Text style={[styles.badgeText, { color: item.isAvailable && item.remainingQuantity > 0 ? Colors.completedText : Colors.textSecondary }]}>
                    {item.isAvailable && item.remainingQuantity > 0 ? 'Available' : 'Sold Out'}
                  </Text>
                </View>
              </View>
              <Text style={styles.cardDesc}>{item.pickupStart} – {item.pickupEnd} · {item.date}</Text>
              <View style={styles.cardBottom}>
                <View>
                  <Text style={styles.price}>${Number(item.discountPrice).toFixed(2)}</Text>
                  <Text style={styles.qty}>{item.remainingQuantity}/{item.quantity} left</Text>
                </View>
                <TouchableOpacity style={styles.actionBtn} onPress={() => onDeleteListing(item.id)}>
                  <Ionicons name="trash-outline" size={20} color={Colors.textSecondary} />
                </TouchableOpacity>
              </View>
            </View>
          )}
          ListEmptyComponent={<EmptyState icon="storefront-outline" text="No listings today." />}
        />
      )}

      <TouchableOpacity
        style={styles.fab}
        onPress={() => tab === 'templates' ? openNewTemplate() : openNewListing()}
      >
        <Ionicons name="add" size={28} color="#fff" />
      </TouchableOpacity>

      {/* Template Modal */}
      <Modal visible={templateModal} transparent animationType="slide">
        <KeyboardAvoidingView style={styles.modalOverlay} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
          <ScrollView style={styles.modal} keyboardShouldPersistTaps="handled">
            <Text style={styles.modalTitle}>{editingTemplate ? 'Edit Template' : 'New Template'}</Text>
            <Text style={styles.label}>Name</Text>
            <Controller control={tc} name="name" rules={{ required: true }} render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="Assorted Pastry Box" placeholderTextColor={Colors.textMuted} />
            )} />
            <Text style={styles.label}>Description</Text>
            <Controller control={tc} name="description" render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="Optional" placeholderTextColor={Colors.textMuted} />
            )} />
            <Text style={styles.label}>Original Price ($)</Text>
            <Controller control={tc} name="originalPrice" rules={{ required: true }} render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="18.99" placeholderTextColor={Colors.textMuted} keyboardType="decimal-pad" />
            )} />
            <View style={styles.modalActions}>
              <TouchableOpacity style={styles.cancelBtn} onPress={() => setTemplateModal(false)}>
                <Text style={styles.cancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.saveBtn} onPress={onSaveTemplate} disabled={tis}>
                {tis ? <ActivityIndicator color="#fff" size="small" /> : <Text style={styles.saveBtnText}>Save</Text>}
              </TouchableOpacity>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </Modal>

      {/* Listing Modal */}
      <Modal visible={listingModal} transparent animationType="slide">
        <KeyboardAvoidingView style={styles.modalOverlay} behavior={Platform.OS === 'ios' ? 'padding' : 'height'}>
          <ScrollView style={styles.modal} keyboardShouldPersistTaps="handled">
            <Text style={styles.modalTitle}>New Listing</Text>

            <Text style={styles.label}>Select Template</Text>
            <View style={styles.templatePicker}>
              {templates.filter(t => t.isActive).map(t => (
                <TouchableOpacity
                  key={t.id}
                  style={[styles.templateOption, selectedTemplateId === t.id && styles.templateOptionSelected]}
                  onPress={() => setSelectedTemplateId(t.id)}
                >
                  <Text style={[styles.templateOptionText, selectedTemplateId === t.id && styles.templateOptionTextSelected]}>
                    {t.name}
                  </Text>
                  <Text style={[styles.templateOptionPrice, selectedTemplateId === t.id && styles.templateOptionTextSelected]}>
                    ${Number(t.originalPrice).toFixed(2)}
                  </Text>
                </TouchableOpacity>
              ))}
              {templates.filter(t => t.isActive).length === 0 && (
                <Text style={styles.noTemplates}>No active templates. Create a template first.</Text>
              )}
            </View>

            <Text style={styles.label}>Discount Price ($)</Text>
            <Controller control={lc} name="discountPrice" rules={{ required: true }} render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="6.99" placeholderTextColor={Colors.textMuted} keyboardType="decimal-pad" />
            )} />
            <Text style={styles.label}>Quantity</Text>
            <Controller control={lc} name="quantity" rules={{ required: true }} render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="10" placeholderTextColor={Colors.textMuted} keyboardType="number-pad" />
            )} />
            <Text style={styles.label}>Pickup Start (HH:MM)</Text>
            <Controller control={lc} name="pickupStart" rules={{ required: true }} render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="18:00" placeholderTextColor={Colors.textMuted} />
            )} />
            <Text style={styles.label}>Pickup End (HH:MM)</Text>
            <Controller control={lc} name="pickupEnd" rules={{ required: true }} render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="20:00" placeholderTextColor={Colors.textMuted} />
            )} />
            <Text style={styles.label}>Date (YYYY-MM-DD)</Text>
            <Controller control={lc} name="date" rules={{ required: true }} render={({ field: { onChange, value } }) => (
              <TextInput style={styles.input} value={value} onChangeText={onChange} placeholder="2026-04-14" placeholderTextColor={Colors.textMuted} />
            )} />
            <View style={styles.modalActions}>
              <TouchableOpacity style={styles.cancelBtn} onPress={() => setListingModal(false)}>
                <Text style={styles.cancelText}>Cancel</Text>
              </TouchableOpacity>
              <TouchableOpacity style={styles.saveBtn} onPress={onSaveListing} disabled={lis}>
                {lis ? <ActivityIndicator color="#fff" size="small" /> : <Text style={styles.saveBtnText}>Create</Text>}
              </TouchableOpacity>
            </View>
          </ScrollView>
        </KeyboardAvoidingView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, paddingTop: 60 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text, paddingHorizontal: 20, marginBottom: 16 },
  tabs: { flexDirection: 'row', paddingHorizontal: 20, marginBottom: 12, gap: 8 },
  tab: { paddingHorizontal: 16, paddingVertical: 8, borderRadius: 20, backgroundColor: Colors.border },
  tabActive: { backgroundColor: Colors.primary },
  tabText: { fontSize: 13, color: Colors.textSecondary, fontWeight: '500' },
  tabTextActive: { color: '#fff' },
  list: { paddingHorizontal: 20, paddingBottom: 80, gap: 12 },
  card: {
    backgroundColor: Colors.card, borderRadius: 14, padding: 16,
    shadowColor: '#000', shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05, shadowRadius: 4, elevation: 2,
  },
  cardTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 4 },
  cardName: { fontSize: 15, fontWeight: '600', color: Colors.text, flex: 1, marginRight: 8 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 20 },
  badgeText: { fontSize: 12, fontWeight: '600' },
  cardDesc: { fontSize: 12, color: Colors.textSecondary, marginBottom: 10 },
  cardBottom: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  price: { fontSize: 16, fontWeight: '700', color: Colors.primary },
  qty: { fontSize: 12, color: Colors.textSecondary, marginTop: 2 },
  actions: { flexDirection: 'row', gap: 4 },
  actionBtn: { padding: 6 },
  fab: {
    position: 'absolute', bottom: 24, right: 24,
    width: 52, height: 52, borderRadius: 26,
    backgroundColor: Colors.primary,
    justifyContent: 'center', alignItems: 'center',
    shadowColor: Colors.primary, shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3, shadowRadius: 8, elevation: 6,
  },
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)', justifyContent: 'flex-end' },
  modal: { backgroundColor: Colors.card, borderTopLeftRadius: 20, borderTopRightRadius: 20, padding: 24, maxHeight: '85%' },
  modalTitle: { fontSize: 18, fontWeight: '700', color: Colors.text, marginBottom: 16 },
  label: { fontSize: 13, color: Colors.text, marginBottom: 6, marginTop: 10 },
  input: {
    backgroundColor: Colors.background, borderRadius: 12,
    paddingHorizontal: 16, paddingVertical: 13,
    fontSize: 15, color: Colors.text,
    borderWidth: 1, borderColor: Colors.border,
  },
  templatePicker: { gap: 8, marginBottom: 4 },
  templateOption: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    padding: 12, borderRadius: 10,
    backgroundColor: Colors.background,
    borderWidth: 1, borderColor: Colors.border,
  },
  templateOptionSelected: { borderColor: Colors.primary, backgroundColor: Colors.primaryLight },
  templateOptionText: { fontSize: 14, color: Colors.text, fontWeight: '500' },
  templateOptionTextSelected: { color: Colors.primary },
  templateOptionPrice: { fontSize: 13, color: Colors.textSecondary },
  noTemplates: { fontSize: 13, color: Colors.textSecondary, textAlign: 'center', paddingVertical: 12 },
  modalActions: { flexDirection: 'row', gap: 12, marginTop: 20, marginBottom: 20 },
  cancelBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: Colors.border, alignItems: 'center' },
  cancelText: { fontSize: 15, color: Colors.textSecondary, fontWeight: '500' },
  saveBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: Colors.primary, alignItems: 'center' },
  saveBtnText: { fontSize: 15, color: '#fff', fontWeight: '600' },
});
