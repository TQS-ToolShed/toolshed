import type { FormEvent } from 'react';
import { Search, MapPin, RotateCcw } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export interface ToolSearchBarProps {
  keyword: string;
  location: string;
  onKeywordChange: (value: string) => void;
  onLocationChange: (value: string) => void;
  onSearch: (event?: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  isLoading?: boolean;
  className?: string;
}

export function ToolSearchBar({
  keyword,
  location,
  onKeywordChange,
  onLocationChange,
  onSearch,
  onReset,
  isLoading = false,
  className = '',
}: ToolSearchBarProps) {
  const hasFilters = keyword.trim() || location.trim();

  return (
    <form
      onSubmit={onSearch}
      className={`flex flex-col md:flex-row gap-3 ${className}`}
    >
      {/* Keyword Input */}
      <div className="relative flex-1 md:flex-2">
        <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
        <Input
          placeholder="Search tools by name, category..."
          value={keyword}
          onChange={(e) => onKeywordChange(e.target.value)}
          disabled={isLoading}
          className="pl-10"
        />
      </div>

      {/* Location Input */}
      <div className="relative flex-1">
        <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
        <Input
          placeholder="Location (e.g., Aveiro)"
          value={location}
          onChange={(e) => onLocationChange(e.target.value)}
          disabled={isLoading}
          className="pl-10"
        />
      </div>

      {/* Action Buttons */}
      <div className="flex gap-2">
        <Button type="submit" disabled={isLoading} className="flex-1 md:flex-none">
          <Search className="h-4 w-4 mr-2" />
          {isLoading ? 'Searching...' : 'Search'}
        </Button>
        {hasFilters && (
          <Button
            type="button"
            variant="outline"
            onClick={onReset}
            disabled={isLoading}
            className="flex-1 md:flex-none"
          >
            <RotateCcw className="h-4 w-4 mr-2" />
            Reset
          </Button>
        )}
      </div>
    </form>
  );
}
