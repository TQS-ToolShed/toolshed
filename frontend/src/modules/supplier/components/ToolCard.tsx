import { Card, CardContent, CardFooter, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import type { Tool } from '../api/tools-api';

interface ToolCardProps {
  tool: Tool;
  onEdit: (tool: Tool) => void;
  onToggleActive: (tool: Tool) => void;
  onDelete: (toolId: string) => void;
}

export const ToolCard = ({ tool, onEdit, onToggleActive, onDelete }: ToolCardProps) => {
  return (
    <Card className="w-full">
      <CardHeader>
        <div className="flex justify-between items-start">
          <div>
            <CardTitle className="text-lg">{tool.title}</CardTitle>
            <CardDescription className="mt-1">{tool.location}</CardDescription>
          </div>
          <span className={`px-2 py-1 text-xs rounded-full ${tool.active ? 'bg-green-100 text-green-800' : 'bg-gray-100 text-gray-800'}`}>
            {tool.active ? 'Active' : 'Inactive'}
          </span>
        </div>
      </CardHeader>
      <CardContent>
        <p className="text-sm text-muted-foreground line-clamp-2 mb-4">
          {tool.description}
        </p>
        <div className="flex justify-between items-center text-sm">
          <span className="font-semibold text-primary">
            €{tool.pricePerDay.toFixed(2)}/day
          </span>
          <div className="flex items-center gap-1 text-muted-foreground">
            <span>★ {tool.overallRating.toFixed(1)}</span>
            <span>({tool.numRatings} reviews)</span>
          </div>
        </div>
      </CardContent>
      <CardFooter className="flex gap-2">
        <Button
          variant={tool.active ? 'secondary' : 'default'}
          size="sm"
          onClick={() => onToggleActive(tool)}
          className="flex-1"
        >
          {tool.active ? 'Set Inactive' : 'Set Active'}
        </Button>
        <Button variant="outline" size="sm" onClick={() => onEdit(tool)} className="flex-1">
          Edit
        </Button>
        <Button variant="destructive" size="sm" onClick={() => onDelete(tool.id)} className="flex-1">
          Delete
        </Button>
      </CardFooter>
    </Card>
  );
};
