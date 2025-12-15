import { useState } from "react";
import { Button } from "@/components/ui/button";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import type { Tool } from "../api/tools-api";

interface ToolMaintenanceDialogProps {
  tool: Tool | null;
  isOpen: boolean;
  onClose: () => void;
  onSave: (toolId: string, availableDate: string | null) => Promise<void>;
}

export const ToolMaintenanceDialog = ({
  tool,
  isOpen,
  onClose,
  onSave,
}: ToolMaintenanceDialogProps) => {
  const [date, setDate] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const handleSave = async () => {
    if (!tool) return;
    try {
      setIsLoading(true);
      await onSave(tool.id, date);
      onClose();
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleClear = async () => {
    if (!tool) return;
    try {
      setIsLoading(true);
      await onSave(tool.id, null);
      onClose();
    } catch (error) {
      console.error(error);
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen || !tool) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="bg-background rounded-lg shadow-lg max-w-md w-full p-6 border">
        <div className="mb-4">
          <h2 className="text-lg font-semibold">Maintenance Schedule</h2>
          <p className="text-sm text-muted-foreground mt-1">
            Set a maintenance schedule for <strong>{tool.title}</strong>.
          </p>
        </div>

        <div className="grid gap-4 py-4">
          <div className="grid grid-cols-4 items-center gap-4">
            <Label htmlFor="availableDate" className="text-right">
              Available Date
            </Label>
            <Input
              id="availableDate"
              type="date"
              className="col-span-3"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              min={new Date().toISOString().split("T")[0]}
            />
          </div>
          {tool.underMaintenance && (
            <div className="text-sm text-yellow-600 dark:text-yellow-400">
              Currently under maintenance until: {tool.maintenanceAvailableDate}
            </div>
          )}
        </div>

        <div className="flex justify-between mt-6">
          <Button
            variant="outline"
            type="button"
            onClick={handleClear}
            disabled={isLoading}
          >
            Clear
          </Button>
          <div className="flex gap-2">
            <Button variant="ghost" onClick={onClose} disabled={isLoading}>
              Cancel
            </Button>
            <Button
              type="button"
              onClick={handleSave}
              disabled={isLoading || !date}
            >
              {isLoading ? "Saving..." : "Set Maintenance"}
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
};
