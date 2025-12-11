import type { FormEvent } from 'react';
import { Search, MapPin, RotateCcw, Euro } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

export interface ToolSearchBarProps {
  keyword: string;
  location: string;
  minPrice: string;
  maxPrice: string;
  onKeywordChange: (value: string) => void;
  onLocationChange: (value: string) => void;
  onMinPriceChange: (value: string) => void;
  onMaxPriceChange: (value: string) => void;
  onSearch: (event?: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  isLoading?: boolean;
  className?: string;
}

export function ToolSearchBar({
  keyword,
  location,
  minPrice,
  maxPrice,
  onKeywordChange,
  onLocationChange,
  onMinPriceChange,
  onMaxPriceChange,
  onSearch,
  onReset,
  isLoading = false,
  className = '',
}: ToolSearchBarProps) {
  const hasFilters = keyword.trim() || location.trim() || minPrice.trim() || maxPrice.trim();

  return (
    <form
      onSubmit={onSearch}
      className={`flex flex-col gap-3 ${className}`}
    >
      {/* Row 1: Keyword and Location */}
      <div className="flex flex-col md:flex-row gap-3">
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
      </div>

      {/* Row 2: Price Range and Actions */}
      <div className="flex flex-col md:flex-row gap-3">
        {/* Min Price Input */}
        <div className="relative flex-1">
          <Euro className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <Input
            type="number"
            placeholder="Min price (€/day)"
            value={minPrice}
            onChange={(e) => onMinPriceChange(e.target.value)}
            disabled={isLoading}
            className="pl-10"
            min="0"
            step="0.01"
          />
        </div>

        {/* Max Price Input */}
        <div className="relative flex-1">
          <Euro className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <Input
            type="number"
            placeholder="Max price (€/day)"
            value={maxPrice}
            onChange={(e) => onMaxPriceChange(e.target.value)}
            disabled={isLoading}
            className="pl-10"
            min="0"
            step="0.01"
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
      </div>
    </form>
  );
}
