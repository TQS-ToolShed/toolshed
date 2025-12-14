import { Link } from 'react-router-dom';
import { Card, CardContent, CardFooter, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Star } from 'lucide-react';
import type { Tool } from '@/modules/supplier/api/tools-api';

interface AvailableToolCardProps {
  tool: Tool;
  isFavorite?: boolean;
  onToggleFavorite?: (toolId: string) => void;
}

interface StarRatingProps {
  rating: number;
  size?: number;
  showCount?: boolean;
  count?: number;
}

const StarRating = ({ rating, size = 16, showCount = false, count = 0 }: StarRatingProps) => {
  const rounded = Math.round((rating || 0) * 2) / 2; // round to nearest 0.5
  const stars = Array.from({ length: 5 }, (_, i) => {
    const filled = i + 1 <= Math.floor(rounded);
    const half = !filled && i + 0.5 === rounded;
    return (
      <Star
        key={i}
        className={filled ? 'text-yellow-500 fill-yellow-400' : 'text-muted-foreground'}
        style={{ width: size, height: size }}
      />
    );
  });
  return (
    <div className="flex items-center gap-1">
      {stars}
      {showCount && (
        <span className="text-xs text-muted-foreground">({count ?? 0})</span>
      )}
    </div>
  );
};

export const AvailableToolCard = ({ tool, isFavorite = false, onToggleFavorite }: AvailableToolCardProps) => {
  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex justify-between items-start gap-2">
          <div>
            <CardTitle className="text-lg">{tool.title}</CardTitle>
            <CardDescription className="mt-1">
              {tool.district}
            </CardDescription>
          </div>
          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={() => onToggleFavorite?.(tool.id)}
              aria-label={isFavorite ? 'Remove from favorites' : 'Add to favorites'}
              className="rounded-full p-2 hover:bg-muted transition-colors"
            >
              {isFavorite ? (
                <Star className="h-4 w-4 fill-yellow-400 text-yellow-500" />
              ) : (
                <Star className="h-4 w-4 text-muted-foreground" />
              )}
            </button>
            <span className="px-2 py-1 text-xs rounded-full bg-emerald-100 text-emerald-800">
              Available
            </span>
          </div>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground line-clamp-2">
          {tool.description}
        </p>
        <div className="flex justify-between text-sm">
          <div className="font-semibold text-primary">
            €{tool.pricePerDay.toFixed(2)}/day
          </div>
          <div className="flex items-center gap-1 text-muted-foreground">
            <StarRating
              rating={tool.overallRating}
              showCount
              count={tool.numRatings}
              size={14}
            />
          </div>
        </div>
      </CardContent>
      <CardFooter className="flex justify-between items-center">
        <div className="text-sm text-muted-foreground">
          Availability updated daily
        </div>
        <Button size="sm" variant="secondary" asChild>
          <Link to={`/renter/bookings/${tool.id}`}>View details</Link>
        </Button>
      </CardFooter>
    </Card>
  );
};
