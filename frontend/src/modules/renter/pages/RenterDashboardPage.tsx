import { useCallback, useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { Button } from '@/components/ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ToolSearchBar } from '@/modules/shared/components/ToolSearchBar';
import { AvailableToolCard } from '../components/AvailableToolCard';
import { getActiveTools, searchTools, type Tool } from '@/modules/supplier/api/tools-api';
import { RenterNavbar } from '../components/RenterNavbar';

export const RenterDashboardPage = () => {
  const [tools, setTools] = useState<Tool[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSearching, setIsSearching] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [keyword, setKeyword] = useState('');
  const [location, setLocation] = useState('');

  const fetchTools = useCallback(
    async (filters?: { keyword?: string; location?: string }) => {
      try {
        setIsLoading(true);
        setError(null);

        const hasFilters = Boolean(filters?.keyword || filters?.location);
        const data: Tool[] = hasFilters
          ? await searchTools(filters?.keyword, filters?.location)
          : await getActiveTools();

        setTools(data.filter((tool: Tool) => tool.active));
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to load tools');
      } finally {
        setIsLoading(false);
      }
    },
    []
  );

  useEffect(() => {
    fetchTools();
  }, [fetchTools]);

  const handleSearch = async (event?: FormEvent<HTMLFormElement>) => {
    event?.preventDefault();
    const keywordQuery = keyword.trim();
    const locationQuery = location.trim();

    setIsSearching(true);
    await fetchTools({
      keyword: keywordQuery || undefined,
      location: locationQuery || undefined,
    });
    setIsSearching(false);
  };

  const handleResetFilters = async () => {
    setKeyword('');
    setLocation('');
    setIsSearching(true);
    await fetchTools();
    setIsSearching(false);
  };

  const averagePrice = useMemo(() => {
    if (!tools.length) return 0;
    const total = tools.reduce((sum, tool) => sum + tool.pricePerDay, 0);
    return total / tools.length;
  }, [tools]);

  return (
    <div className="min-h-screen bg-background">

      <RenterNavbar />

      {/* Main Content */}
      <main className="container mx-auto py-8 px-4">
        <div className="mb-8">
          <h2 className="text-3xl font-bold mb-2">Browse tools ready to rent</h2>
          <p className="text-muted-foreground">
            Discover gear from nearby suppliers and book what you need.
          </p>
        </div>

        {/* Stats */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
          <Card>
            <CardHeader>
              <CardTitle>Available tools</CardTitle>
              <CardDescription>Everything currently active in the marketplace</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold">{tools.length}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Average daily rate</CardTitle>
              <CardDescription>What renters typically pay per day</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold">€{averagePrice.toFixed(2)}</p>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>Availability</CardTitle>
              <CardDescription>Tools marked as bookable right now</CardDescription>
            </CardHeader>
            <CardContent>
              <p className="text-3xl font-bold text-emerald-600">Open</p>
            </CardContent>
          </Card>
        </div>

        {/* Search */}
        <Card className="mb-6">
          <CardHeader>
            <CardTitle>Find the right tool</CardTitle>
            <CardDescription>Search by keyword or narrow down to a location</CardDescription>
          </CardHeader>
          <CardContent>
            <ToolSearchBar
              keyword={keyword}
              location={location}
              onKeywordChange={setKeyword}
              onLocationChange={setLocation}
              onSearch={handleSearch}
              onReset={handleResetFilters}
              isLoading={isSearching || isLoading}
            />
          </CardContent>
        </Card>

        {error && (
          <div className="bg-destructive/10 border border-destructive text-destructive px-4 py-3 rounded-lg mb-6">
            {error}
          </div>
        )}

        {isLoading ? (
          <div className="flex items-center justify-center py-16">
            <p className="text-muted-foreground">Loading available tools...</p>
          </div>
        ) : tools.length === 0 ? (
          <div className="text-center py-16">
            <p className="text-muted-foreground mb-4">
              No tools are available right now. Try adjusting your search or check back soon.
            </p>
            <Button onClick={handleResetFilters} variant="outline">
              Refresh list
            </Button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {tools.map((tool) => (
              <AvailableToolCard key={tool.id} tool={tool} />
            ))}
          </div>
        )}
      </main>
    </div>
  );
};
