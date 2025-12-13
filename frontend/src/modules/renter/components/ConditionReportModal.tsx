import { useState } from 'react';
import { Button } from '@/components/ui/button';
import {
  type ConditionStatus,
  type ConditionReportInput,
  submitConditionReport,
} from '../api/bookings-api';
import { useAuth } from '@/modules/auth/context/AuthContext';

interface ConditionReportModalProps {
  open: boolean;
  onClose: () => void;
  bookingId: string;
  toolName: string;
  onSubmitted: () => void;
}

const conditionOptions: { value: ConditionStatus; label: string; description: string; requiresDeposit: boolean }[] = [
  { value: 'OK', label: 'OK', description: 'Tool in perfect condition', requiresDeposit: false },
  { value: 'USED', label: 'Used', description: 'Normal wear and tear', requiresDeposit: false },
  { value: 'MINOR_DAMAGE', label: 'Minor Damage', description: 'Small damage requiring repair', requiresDeposit: true },
  { value: 'BROKEN', label: 'Broken', description: 'Tool is broken/unusable', requiresDeposit: true },
  { value: 'MISSING_PARTS', label: 'Missing Parts', description: 'Parts are missing from the tool', requiresDeposit: true },
];

export const ConditionReportModal = ({
  open,
  onClose,
  bookingId,
  toolName,
  onSubmitted,
}: ConditionReportModalProps) => {
  const { user } = useAuth();
  const [selectedCondition, setSelectedCondition] = useState<ConditionStatus | null>(null);
  const [description, setDescription] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selectedOption = conditionOptions.find((o) => o.value === selectedCondition);

  const handleSubmit = async () => {
    if (!selectedCondition || !user?.id) return;

    try {
      setIsSubmitting(true);
      setError(null);

      const input: ConditionReportInput = {
        conditionStatus: selectedCondition,
        description: description.trim() || undefined,
        renterId: user.id,
      };

      await submitConditionReport(bookingId, input);
      onSubmitted();
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to submit condition report');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleClose = () => {
    setSelectedCondition(null);
    setDescription('');
    setError(null);
    onClose();
  };

  if (!open) return null;

  return (
    <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50 backdrop-blur-sm px-4">
      <div className="bg-background border border-border rounded-xl shadow-xl w-full max-w-lg overflow-hidden">
        <div className="px-5 py-4 border-b border-border">
          <p className="text-sm text-muted-foreground">Condition Report</p>
          <h2 className="text-xl font-semibold">{toolName}</h2>
        </div>

        <div className="p-5 space-y-4">
          {error && (
            <div className="bg-destructive/10 border border-destructive text-destructive px-3 py-2 rounded-md text-sm">
              {error}
            </div>
          )}

          <div className="space-y-2">
            <label className="text-sm font-medium">How was the tool condition?</label>
            <div className="grid grid-cols-1 gap-2">
              {conditionOptions.map((option) => (
                <button
                  key={option.value}
                  type="button"
                  onClick={() => setSelectedCondition(option.value)}
                  className={`p-3 rounded-lg border text-left transition-colors ${
                    selectedCondition === option.value
                      ? 'border-primary bg-primary/10'
                      : 'border-border hover:border-primary/50'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <span className="font-medium">{option.label}</span>
                      <p className="text-xs text-muted-foreground">{option.description}</p>
                    </div>
                    {option.requiresDeposit && (
                      <span className="text-xs px-2 py-1 rounded-full bg-amber-100 text-amber-800">
                        Requires deposit
                      </span>
                    )}
                  </div>
                </button>
              ))}
            </div>
          </div>

          {selectedOption?.requiresDeposit && (
            <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg">
              <p className="text-sm text-amber-800">
                <strong>Note:</strong> Reporting damage will require a €50.00 security deposit.
              </p>
            </div>
          )}

          <div className="space-y-2">
            <label className="text-sm font-medium">
              Additional notes <span className="text-muted-foreground">(optional)</span>
            </label>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe any issues or notes about the tool condition..."
              className="w-full h-24 px-3 py-2 rounded-md border border-input bg-background text-sm resize-none focus:outline-none focus:ring-2 focus:ring-primary"
              maxLength={500}
            />
            <p className="text-xs text-muted-foreground text-right">{description.length}/500</p>
          </div>
        </div>

        <div className="px-5 py-4 border-t border-border flex justify-end gap-3">
          <Button variant="outline" onClick={handleClose} disabled={isSubmitting}>
            Cancel
          </Button>
          <Button
            onClick={handleSubmit}
            disabled={!selectedCondition || isSubmitting}
          >
            {isSubmitting ? 'Submitting...' : 'Submit Report'}
          </Button>
        </div>
      </div>
    </div>
  );
};
