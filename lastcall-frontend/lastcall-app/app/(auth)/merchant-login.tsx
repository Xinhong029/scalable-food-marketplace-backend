import { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, StyleSheet, Alert, ActivityIndicator } from 'react-native';
import { useRouter } from 'expo-router';
import { useForm, Controller } from 'react-hook-form';
import { Ionicons } from '@expo/vector-icons';
import { loginMerchant } from '../../api/auth';
import { useAuthStore } from '../../store/authStore';
import { Colors } from '../../constants/colors';

interface FormData { email: string; password: string; }

export default function MerchantLoginScreen() {
  const router = useRouter();
  const { setAuth } = useAuthStore();
  const [loading, setLoading] = useState(false);
  const { control, handleSubmit } = useForm<FormData>();

  const onSubmit = async (data: FormData) => {
    setLoading(true);
    try {
      const res = await loginMerchant(data);
      const { accessToken, userId, role } = res.data.data;
      await setAuth(accessToken, userId, role);
      router.replace('/(merchant)');
    } catch (e: any) {
      Alert.alert('Login failed', e.response?.data?.message || 'Please check your credentials');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <TouchableOpacity style={styles.back} onPress={() => router.back()}>
        <Ionicons name="arrow-back" size={24} color={Colors.text} />
      </TouchableOpacity>

      <View style={styles.iconBox}>
        <Ionicons name="storefront" size={24} color="#fff" />
      </View>

      <Text style={styles.title}>Merchant Login</Text>
      <Text style={styles.subtitle}>Welcome back!</Text>

      <View style={styles.form}>
        <Text style={styles.label}>Email</Text>
        <Controller control={control} name="email" rules={{ required: true }} render={({ field: { onChange, value } }) => (
          <TextInput style={styles.input} placeholder="you@example.com" placeholderTextColor={Colors.textMuted} keyboardType="email-address" autoCapitalize="none" value={value} onChangeText={onChange} />
        )} />

        <Text style={styles.label}>Password</Text>
        <Controller control={control} name="password" rules={{ required: true }} render={({ field: { onChange, value } }) => (
          <TextInput style={styles.input} placeholder="••••••••" placeholderTextColor={Colors.textMuted} secureTextEntry value={value} onChangeText={onChange} />
        )} />

        <TouchableOpacity style={styles.button} onPress={handleSubmit(onSubmit)} disabled={loading} activeOpacity={0.85}>
          {loading ? <ActivityIndicator color="#fff" /> : <Text style={styles.buttonText}>Login as Merchant</Text>}
        </TouchableOpacity>

        <View style={styles.footer}>
          <Text style={styles.footerText}>Don't have an account? </Text>
          <TouchableOpacity onPress={() => router.push('/(auth)/merchant-register')}>
            <Text style={styles.link}>Register</Text>
          </TouchableOpacity>
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: Colors.background, paddingHorizontal: 28, paddingTop: 60 },
  back: { marginBottom: 24 },
  iconBox: { width: 52, height: 52, borderRadius: 14, backgroundColor: Colors.secondary, justifyContent: 'center', alignItems: 'center', marginBottom: 16 },
  title: { fontSize: 26, fontWeight: '700', color: Colors.text, marginBottom: 4 },
  subtitle: { fontSize: 14, color: Colors.textSecondary, marginBottom: 32 },
  form: { gap: 8 },
  label: { fontSize: 13, color: Colors.text, marginBottom: 4, marginTop: 8 },
  input: { backgroundColor: Colors.card, borderRadius: 12, paddingHorizontal: 16, paddingVertical: 14, fontSize: 15, color: Colors.text, borderWidth: 1, borderColor: Colors.border },
  button: { backgroundColor: Colors.secondary, borderRadius: 12, paddingVertical: 16, alignItems: 'center', marginTop: 24 },
  buttonText: { color: '#fff', fontSize: 16, fontWeight: '600' },
  footer: { flexDirection: 'row', justifyContent: 'center', marginTop: 20 },
  footerText: { color: Colors.textSecondary, fontSize: 14 },
  link: { color: Colors.secondary, fontSize: 14, fontWeight: '600' },
});
