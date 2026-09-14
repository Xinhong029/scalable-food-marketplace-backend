import { useEffect, useState } from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ActivityIndicator, TextInput, Switch, Alert, ScrollView } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { getMerchantProfile, updateMerchantProfile, MerchantProfileResponse } from '../../api/merchant';
import { useAuthStore } from '../../store/authStore';
import { Colors } from '../../constants/colors';
import LoadingCenter from '../../components/LoadingCenter';

export default function MerchantProfileScreen() {
  const { clearAuth } = useAuthStore();
  const [profile, setProfile] = useState<MerchantProfileResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [name, setName] = useState('');
  const [address, setAddress] = useState('');
  const [businessHours, setBusinessHours] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [editing, setEditing] = useState(false);

  useEffect(() => {
    getMerchantProfile().then(res => {
      const p = res.data.data;
      setProfile(p);
      setName(p.name);
      setAddress(p.address);
      setBusinessHours(p.businessHours || '');
      setIsActive(p.isActive);
    }).finally(() => setLoading(false));
  }, []);

  const handleSave = async () => {
    setSaving(true);
    try {
      const res = await updateMerchantProfile({ name, address, businessHours });
      setProfile(res.data.data);
      setEditing(false);
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Failed to update');
    } finally {
      setSaving(false);
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
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.header}>
        <Text style={styles.title}>Profile</Text>
        <TouchableOpacity onPress={handleLogout}>
          <Ionicons name="log-out-outline" size={24} color={Colors.textSecondary} />
        </TouchableOpacity>
      </View>

      <View style={styles.card}>
        <Field label="Store Name" value={name} onChange={setName} editing={editing} />
        <Field label="Address" value={address} onChange={setAddress} editing={editing} />
        <Field label="Business Hours" value={businessHours} onChange={setBusinessHours} editing={editing} placeholder="06:00 – 22:00" />
        <View style={styles.switchRow}>
          <View>
            <Text style={styles.fieldLabel}>Store Status</Text>
            <Text style={styles.fieldValue}>{isActive ? 'Active' : 'Inactive'}</Text>
          </View>
          <Switch value={isActive} onValueChange={setIsActive} trackColor={{ true: Colors.secondary, false: Colors.border }} thumbColor="#fff" />
        </View>
      </View>

      {editing ? (
        <View style={styles.btnRow}>
          <TouchableOpacity style={styles.cancelBtn} onPress={() => setEditing(false)}>
            <Text style={styles.cancelText}>Cancel</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.saveBtn} onPress={handleSave} disabled={saving}>
            {saving ? <ActivityIndicator color="#fff" size="small" /> : <Text style={styles.saveBtnText}>Save</Text>}
          </TouchableOpacity>
        </View>
      ) : (
        <TouchableOpacity style={styles.editBtn} onPress={() => setEditing(true)}>
          <Text style={styles.editBtnText}>Edit Profile</Text>
        </TouchableOpacity>
      )}
    </ScrollView>
  );
}

function Field({ label, value, onChange, editing, placeholder }: { label: string; value: string; onChange: (v: string) => void; editing: boolean; placeholder?: string }) {
  return (
    <View style={fieldStyles.row}>
      <Text style={fieldStyles.label}>{label}</Text>
      {editing ? (
        <TextInput style={fieldStyles.input} value={value} onChangeText={onChange} placeholder={placeholder} placeholderTextColor={Colors.textMuted} />
      ) : (
        <Text style={fieldStyles.value}>{value || '—'}</Text>
      )}
    </View>
  );
}

const fieldStyles = StyleSheet.create({
  row: { paddingVertical: 14, borderBottomWidth: 1, borderBottomColor: Colors.border },
  label: { fontSize: 12, color: Colors.textSecondary, marginBottom: 4 },
  value: { fontSize: 15, fontWeight: '500', color: Colors.text },
  input: { fontSize: 15, color: Colors.text, backgroundColor: Colors.background, borderRadius: 8, paddingHorizontal: 12, paddingVertical: 8, borderWidth: 1, borderColor: Colors.border },
});

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background },
  content: { paddingTop: 60, paddingHorizontal: 20, paddingBottom: 40 },
  header: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text },
  card: { backgroundColor: Colors.card, borderRadius: 14, paddingHorizontal: 16, shadowColor: '#000', shadowOffset: { width: 0, height: 1 }, shadowOpacity: 0.05, shadowRadius: 4, elevation: 2, marginBottom: 20 },
  switchRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', paddingVertical: 14 },
  fieldLabel: { fontSize: 12, color: Colors.textSecondary, marginBottom: 4 },
  fieldValue: { fontSize: 15, fontWeight: '500', color: Colors.text },
  btnRow: { flexDirection: 'row', gap: 12 },
  cancelBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: Colors.border, alignItems: 'center' },
  cancelText: { fontSize: 15, color: Colors.textSecondary, fontWeight: '500' },
  saveBtn: { flex: 1, paddingVertical: 14, borderRadius: 12, backgroundColor: Colors.primary, alignItems: 'center' },
  saveBtnText: { fontSize: 15, color: '#fff', fontWeight: '600' },
  editBtn: { paddingVertical: 14, borderRadius: 12, backgroundColor: Colors.primaryLight, alignItems: 'center' },
  editBtnText: { color: Colors.primary, fontSize: 15, fontWeight: '600' },
});
