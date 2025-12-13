import type { FormEvent } from 'react';
import { useEffect, useState } from 'react';
import { Search, MapPin, RotateCcw, Euro } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { fetchDistricts } from '@/modules/shared/api/geo-api';

export interface ToolSearchBarProps {
  keyword: string;
  district: string;
  minPrice: string;
  maxPrice: string;
  onKeywordChange: (value: string) => void;
  onDistrictChange: (value: string) => void;
  onMinPriceChange: (value: string) => void;
  onMaxPriceChange: (value: string) => void;
  onSearch: (event?: FormEvent<HTMLFormElement>) => void;
  onReset: () => void;
  isLoading?: boolean;
  className?: string;
}

export function ToolSearchBar({
  keyword,
  district,
  minPrice,
  maxPrice,
  onKeywordChange,
  onDistrictChange,
  onMinPriceChange,
  onMaxPriceChange,
  onSearch,
  onReset,
  isLoading = false,
  className = '',
}: ToolSearchBarProps) {
  const [districtOptions, setDistrictOptions] = useState<string[]>([]);
  const [isGeoLoading, setIsGeoLoading] = useState(false);

  useEffect(() => {
    let isMounted = true;

    const loadDistricts = async () => {
      try {
        setIsGeoLoading(true);
        const districts = await fetchDistricts();
        if (isMounted) {
          setDistrictOptions(districts);
        }
      } finally {
        if (isMounted) {
          setIsGeoLoading(false);
        }
      }
    };

    loadDistricts();

    return () => {
      isMounted = false;
    };
  }, []);

  const effectiveLoading = isLoading || isGeoLoading;
  const hasFilters = keyword.trim() || district.trim() || minPrice.trim() || maxPrice.trim();

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
            disabled={effectiveLoading}
            className="pl-10"
          />
        </div>

        {/* District select */}
        <div className="relative flex-1">
          <MapPin className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground pointer-events-none" />
          <select
            value={district}
            onChange={(e) => onDistrictChange(e.target.value)}
            disabled={effectiveLoading}
            className="pl-10 pr-3 w-full rounded-md border border-input bg-background px-3 py-2 text-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring cursor-pointer disabled:cursor-not-allowed disabled:opacity-50"
          >
            <option value="">All districts</option>
            {districtOptions.map((d) => (
              <option key={d} value={d}>{d}</option>
            ))}
          </select>
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
            disabled={effectiveLoading}
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
            disabled={effectiveLoading}
            className="pl-10"
            min="0"
            step="0.01"
          />
        </div>

        {/* Action Buttons */}
        <div className="flex gap-2">
          <Button type="submit" disabled={effectiveLoading} className="flex-1 md:flex-none">
            <Search className="h-4 w-4 mr-2" />
            {effectiveLoading ? 'Searching...' : 'Search'}
          </Button>
          {hasFilters && (
            <Button
              type="button"
              variant="outline"
              onClick={onReset}
              disabled={effectiveLoading}
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
