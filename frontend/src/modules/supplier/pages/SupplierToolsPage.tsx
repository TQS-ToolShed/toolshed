import { useState, useEffect, useCallback } from 'react';
import { useAuth } from '@/modules/auth/context/AuthContext';
import { Button } from '@/components/ui/button';
import { ToolCard } from '../components/ToolCard';
import { ToolForm } from '../components/ToolForm';
import { getAllTools, createTool, updateTool, deleteTool } from '../api/tools-api';
import type { Tool, CreateToolInput, UpdateToolInput } from '../api/tools-api';
import { SupplierNavbar } from '../components/SupplierNavbar';

export const SupplierToolsPage = () => {
  const { user } = useAuth();
  const [tools, setTools] = useState<Tool[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [showForm, setShowForm] = useState(false);
  const [editingTool, setEditingTool] = useState<Tool | null>(null);

  // The supplier's ID is used as the owner ID for tools
  const supplierId = user?.id || '';

  const loadTools = useCallback(async () => {
    try {
      setIsLoading(true);
      setError(null);
      const allTools = await getAllTools();
      // Filter to show only tools owned by this supplier
      // Note: Since the backend doesn't return owner info in the Tool response (JsonBackReference),
      // we might need a backend endpoint to get tools by owner. For now, showing all tools.
      // TODO: Add backend endpoint GET /api/tools/supplier/{supplierId}
      setTools(allTools);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load tools');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    loadTools();
  }, [loadTools]);

  const handleCreateTool = async (data: CreateToolInput | UpdateToolInput) => {
    try {
      setIsSubmitting(true);
      setError(null);
      await createTool(data as CreateToolInput);
      setShowForm(false);
      await loadTools();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create tool');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleUpdateTool = async (data: CreateToolInput | UpdateToolInput) => {
    if (!editingTool) return;
    
    try {
      setIsSubmitting(true);
      setError(null);
      await updateTool(editingTool.id, data as UpdateToolInput);
      setEditingTool(null);
      setShowForm(false);
      await loadTools();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update tool');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDeleteTool = async (toolId: string) => {
    if (!confirm('Are you sure you want to delete this tool?')) return;
    
    try {
      setError(null);
      await deleteTool(toolId);
      await loadTools();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to delete tool');
    }
  };

  const handleToggleActive = async (tool: Tool) => {
    try {
      setIsSubmitting(true);
      setError(null);
      await updateTool(tool.id, { active: !tool.active });
      await loadTools();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to update tool status');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleEdit = (tool: Tool) => {
    setEditingTool(tool);
    setShowForm(true);
  };

  const handleCancel = () => {
    setShowForm(false);
    setEditingTool(null);
  };

  const handleAddNew = () => {
    setEditingTool(null);
    setShowForm(true);
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <p className="text-muted-foreground">Loading tools...</p>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
    <SupplierNavbar />
    <div className="container mx-auto py-8 px-4">
      <div className="flex justify-between items-center mb-8">
        <div>
          <h1 className="text-3xl font-bold">My Tools</h1>
          <p className="text-muted-foreground">Manage your tool listings</p>
        </div>
        {!showForm && (
          <Button onClick={handleAddNew}>Add New Tool</Button>
        )}
      </div>

      {error && (
        <div className="bg-destructive/10 border border-destructive text-destructive px-4 py-3 rounded-lg mb-6">
          {error}
        </div>
      )}

      {showForm ? (
        <ToolForm
          tool={editingTool}
          supplierId={supplierId}
          onSubmit={editingTool ? handleUpdateTool : handleCreateTool}
          onCancel={handleCancel}
          isLoading={isSubmitting}
        />
      ) : (
        <>
          {tools.length === 0 ? (
            <div className="text-center py-12">
              <p className="text-muted-foreground mb-4">You haven't added any tools yet.</p>
              <Button onClick={handleAddNew}>Add Your First Tool</Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {tools.map((tool) => (
                <ToolCard
                  key={tool.id}
                  tool={tool}
                  onEdit={handleEdit}
                  onToggleActive={handleToggleActive}
                  onDelete={handleDeleteTool}
                />
              ))}
            </div>
          )}
        </>
      )}
    </div>
    </div>
  );
};
