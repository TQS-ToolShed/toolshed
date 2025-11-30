import { useState, useEffect } from 'react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from '@/components/ui/card';
import type { Tool, CreateToolInput, UpdateToolInput } from '../api/tools-api';

interface ToolFormProps {
  tool?: Tool | null;
  supplierId: string;
  onSubmit: (data: CreateToolInput | UpdateToolInput) => void;
  onCancel: () => void;
  isLoading?: boolean;
}

export const ToolForm = ({ tool, supplierId, onSubmit, onCancel, isLoading }: ToolFormProps) => {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [pricePerDay, setPricePerDay] = useState('');
  const [location, setLocation] = useState('');
  const [active, setActive] = useState(true);

  const isEditing = !!tool;

  useEffect(() => {
    if (tool) {
      setTitle(tool.title);
      setDescription(tool.description);
      setPricePerDay(tool.pricePerDay.toString());
      setLocation(tool.location);
      setActive(tool.active);
    } else {
      setTitle('');
      setDescription('');
      setPricePerDay('');
      setLocation('');
      setActive(true);
    }
  }, [tool]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    
    if (isEditing) {
      const updateData: UpdateToolInput = {
        title,
        description,
        pricePerDay: parseFloat(pricePerDay),
        location,
        active,
      };
      onSubmit(updateData);
    } else {
      const createData: CreateToolInput = {
        title,
        description,
        pricePerDay: parseFloat(pricePerDay),
        location,
        supplierId,
      };
      onSubmit(createData);
    }
  };

  return (
    <Card className="w-full max-w-lg mx-auto">
      <CardHeader>
        <CardTitle>{isEditing ? 'Edit Tool' : 'Add New Tool'}</CardTitle>
      </CardHeader>
      <form onSubmit={handleSubmit}>
        <CardContent className="space-y-4">
          <div className="space-y-2">
            <Label htmlFor="title">Title</Label>
            <Input
              id="title"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Tool title"
              required
            />
          </div>
          
          <div className="space-y-2">
            <Label htmlFor="description">Description</Label>
            <textarea
              id="description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Describe your tool..."
              className="w-full min-h-[100px] rounded-md border border-input bg-transparent px-3 py-2 text-sm placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="pricePerDay">Price per Day (€)</Label>
            <Input
              id="pricePerDay"
              type="number"
              step="0.01"
              min="0"
              value={pricePerDay}
              onChange={(e) => setPricePerDay(e.target.value)}
              placeholder="0.00"
              required
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="location">Location</Label>
            <Input
              id="location"
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              placeholder="City, Country"
              required
            />
          </div>

          {isEditing && (
            <div className="flex items-center space-x-2">
              <input
                type="checkbox"
                id="active"
                checked={active}
                onChange={(e) => setActive(e.target.checked)}
                className="h-4 w-4 rounded border-gray-300"
              />
              <Label htmlFor="active">Active (visible to renters)</Label>
            </div>
          )}
        </CardContent>
        <CardFooter className="flex gap-2">
          <Button type="button" variant="outline" onClick={onCancel} className="flex-1">
            Cancel
          </Button>
          <Button type="submit" disabled={isLoading} className="flex-1">
            {isLoading ? 'Saving...' : isEditing ? 'Update Tool' : 'Create Tool'}
          </Button>
        </CardFooter>
      </form>
    </Card>
  );
};
