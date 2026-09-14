import { Colors } from './colors';

export const ORDER_STATUS_STYLES: Record<string, { bg: string; text: string; label: string }> = {
  PENDING_PAYMENT: { bg: Colors.pending, text: Colors.pendingText, label: 'Pending' },
  PAID: { bg: Colors.paid, text: Colors.paidText, label: 'Paid' },
  COMPLETED: { bg: Colors.completed, text: Colors.completedText, label: 'Completed' },
  CANCELLED: { bg: Colors.cancelled, text: Colors.cancelledText, label: 'Cancelled' },
};
