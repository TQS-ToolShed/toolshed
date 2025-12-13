import { Link } from 'react-router-dom';
import { Card, CardContent, CardFooter, CardHeader, CardTitle, CardDescription } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import type { Tool } from '@/modules/supplier/api/tools-api';

interface AvailableToolCardProps {
  tool: Tool;
}

export const AvailableToolCard = ({ tool }: AvailableToolCardProps) => {
  return (
    <Card className="h-full">
      <CardHeader>
        <div className="flex justify-between items-start">
          <div>
            <CardTitle className="text-lg">{tool.title}</CardTitle>
            <CardDescription className="mt-1">
              {tool.district}
            </CardDescription>
          </div>
          <span className="px-2 py-1 text-xs rounded-full bg-emerald-100 text-emerald-800">
            Available
          </span>
        </div>
      </CardHeader>
      <CardContent className="flex flex-col gap-3">
        <p className="text-sm text-muted-foreground line-clamp-2">{tool.description}</p>
        <div className="flex justify-between text-sm">
          <div className="font-semibold text-primary">€{tool.pricePerDay.toFixed(2)}/day</div>
          <div className="flex items-center gap-1 text-muted-foreground">
            <span>★ {tool.overallRating.toFixed(1)}</span>
            <span>({tool.numRatings} reviews)</span>
          </div>
        </div>
      </CardContent>
      <CardFooter className="flex justify-between items-center">
        <div className="text-sm text-muted-foreground">Availability updated daily</div>
        <Button size="sm" variant="secondary" asChild>
          <Link to={`/renter/bookings/${tool.id}`}>View details</Link>
        </Button>
      </CardFooter>
    </Card>
  );
};
