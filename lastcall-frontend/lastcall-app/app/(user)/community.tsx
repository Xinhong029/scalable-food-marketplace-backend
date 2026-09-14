import { useEffect, useState } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet,
  RefreshControl, TextInput, Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { getPosts, getMyPosts, createPost, deletePost, PostResponse } from '../../api/post';
import { useAuthStore } from '../../store/authStore';
import { Colors } from '../../constants/colors';
import LoadingCenter from '../../components/LoadingCenter';
import EmptyState from '../../components/EmptyState';
import BottomModal from '../../components/BottomModal';

export default function CommunityScreen() {
  const { userId } = useAuthStore();
  const [tab, setTab] = useState<'feed' | 'mine'>('feed');
  const [posts, setPosts] = useState<PostResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [content, setContent] = useState('');
  const [posting, setPosting] = useState(false);

  const load = async () => {
    try {
      if (tab === 'feed') {
        const res = await getPosts();
        setPosts(res.data.data.content);
      } else if (userId) {
        const res = await getMyPosts(userId);
        setPosts(res.data.data.content);
      }
    } catch {
      // silent
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => { setLoading(true); load(); }, [tab]);

  const handlePost = async () => {
    if (!content.trim()) return;
    setPosting(true);
    try {
      await createPost({ content: content.trim() });
      setContent('');
      setModalVisible(false);
      load();
    } catch (e: any) {
      Alert.alert('Error', e.response?.data?.message || 'Failed to post');
    } finally {
      setPosting(false);
    }
  };

  const handleDelete = (postId: number) => {
    Alert.alert('Delete post', 'Are you sure?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete', style: 'destructive', onPress: async () => {
          await deletePost(postId);
          load();
        }
      },
    ]);
  };

  const renderItem = ({ item }: { item: PostResponse }) => (
    <View style={styles.card}>
      <View style={styles.cardHeader}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>{item.userNickname?.[0]?.toUpperCase() || '?'}</Text>
        </View>
        <View style={styles.headerInfo}>
          <Text style={styles.nickname}>{item.userNickname}</Text>
          <Text style={styles.date}>{new Date(item.createdAt).toLocaleDateString()}</Text>
        </View>
        {item.userId === userId && (
          <TouchableOpacity onPress={() => handleDelete(item.id)}>
            <Ionicons name="close-outline" size={20} color={Colors.textMuted} />
          </TouchableOpacity>
        )}
      </View>
      <Text style={styles.postContent}>{item.content}</Text>
      {item.merchantName ? (
        <View style={styles.merchantTag}>
          <Ionicons name="location-outline" size={12} color={Colors.textSecondary} />
          <Text style={styles.merchantTagText}> {item.merchantName}</Text>
        </View>
      ) : null}
      <View style={styles.cardFooter}>
        <View style={styles.statRow}>
          <Ionicons name="heart-outline" size={14} color={Colors.textSecondary} />
          <Text style={styles.stat}> {item.likeCount}</Text>
        </View>
        <View style={styles.statRow}>
          <Ionicons name="chatbubble-outline" size={14} color={Colors.textSecondary} />
          <Text style={styles.stat}> {item.commentCount}</Text>
        </View>
      </View>
    </View>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Community</Text>

      <View style={styles.tabs}>
        <TouchableOpacity
          style={[styles.tab, tab === 'feed' && styles.tabActive]}
          onPress={() => setTab('feed')}
        >
          <Text style={[styles.tabText, tab === 'feed' && styles.tabTextActive]}>Feed</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.tab, tab === 'mine' && styles.tabActive]}
          onPress={() => setTab('mine')}
        >
          <Text style={[styles.tabText, tab === 'mine' && styles.tabTextActive]}>My Posts</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <LoadingCenter />
      ) : (
        <FlatList
          data={posts}
          keyExtractor={(item) => String(item.id)}
          renderItem={renderItem}
          contentContainerStyle={styles.list}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); load(); }} tintColor={Colors.primary} />}
          ListEmptyComponent={<EmptyState icon="chatbubble-outline" text="No posts yet." />}
        />
      )}

      <TouchableOpacity style={styles.fab} onPress={() => setModalVisible(true)}>
        <Text style={styles.fabText}>+</Text>
      </TouchableOpacity>

      <BottomModal
        visible={modalVisible}
        title="New Post"
        onClose={() => setModalVisible(false)}
        onConfirm={handlePost}
        confirmLabel="Post"
        confirming={posting}
      >
        <TextInput
          style={styles.textarea}
          placeholder="Share your food experience..."
          placeholderTextColor={Colors.textMuted}
          multiline
          value={content}
          onChangeText={setContent}
          maxLength={1000}
        />
      </BottomModal>
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
    backgroundColor: Colors.card,
    borderRadius: 14,
    padding: 16,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 1 },
    shadowOpacity: 0.05,
    shadowRadius: 4,
    elevation: 2,
  },
  cardHeader: { flexDirection: 'row', alignItems: 'center', marginBottom: 10 },
  avatar: {
    width: 36, height: 36, borderRadius: 18,
    backgroundColor: Colors.primaryLight,
    justifyContent: 'center', alignItems: 'center', marginRight: 10,
  },
  avatarText: { fontSize: 16, fontWeight: '600', color: Colors.primary },
  headerInfo: { flex: 1 },
  nickname: { fontSize: 14, fontWeight: '600', color: Colors.text },
  date: { fontSize: 11, color: Colors.textSecondary },
  postContent: { fontSize: 14, color: Colors.text, lineHeight: 20, marginBottom: 10 },
  merchantTag: { flexDirection: 'row', alignItems: 'center', marginBottom: 10 },
  merchantTagText: { fontSize: 12, color: Colors.textSecondary },
  cardFooter: { flexDirection: 'row', gap: 16 },
  statRow: { flexDirection: 'row', alignItems: 'center' },
  stat: { fontSize: 13, color: Colors.textSecondary },
  fab: {
    position: 'absolute', bottom: 24, right: 24,
    width: 52, height: 52, borderRadius: 26,
    backgroundColor: Colors.primary,
    justifyContent: 'center', alignItems: 'center',
    shadowColor: Colors.primary, shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.3, shadowRadius: 8, elevation: 6,
  },
  fabText: { fontSize: 26, color: '#fff', fontWeight: '300', marginTop: -2 },
  textarea: {
    backgroundColor: Colors.background,
    borderRadius: 12,
    padding: 14,
    fontSize: 14,
    color: Colors.text,
    minHeight: 120,
    textAlignVertical: 'top',
    marginBottom: 16,
    borderWidth: 1,
    borderColor: Colors.border,
  },
});
