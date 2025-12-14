

import type { MonthlyEarnings } from '../api/payoutApi';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid, Cell } from 'recharts';
import { TrendingUp } from 'lucide-react';

interface EarningsChartProps {
  data: MonthlyEarnings[];
}

// Custom tooltip component
const CustomTooltip = ({ active, payload }: any) => {
  if (active && payload && payload.length) {
    return (
      <div className="bg-popover border border-border rounded-lg shadow-lg p-3">
        <p className="text-sm font-medium text-foreground">{payload[0].payload.name}</p>
        <p className="text-lg font-bold text-primary mt-1">
          €{Number(payload[0].value).toFixed(2)}
        </p>
      </div>
    );
  }
  return null;
};

export function EarningsChart({ data }: EarningsChartProps) {
  const chartData = data.map(item => ({
    name: `${item.month.substring(0, 3)} ${item.year}`,
    amount: item.amount,
    fullMonth: item.month
  }));

  // Calculate total earnings
  const totalEarnings = data.reduce((sum, item) => sum + item.amount, 0);

  if (!data || data.length === 0) {
    return (
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5 text-primary" />
            Earnings Overview
          </CardTitle>
        </CardHeader>
        <CardContent>
          <p className="text-muted-foreground">No earnings data available yet.</p>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="col-span-full">
      <CardHeader>
        <div className="flex items-center justify-between">
          <CardTitle className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5 text-primary" />
            Monthly Earnings
          </CardTitle>
          <div className="text-right">
            <p className="text-sm text-muted-foreground">Total Earnings</p>
            <p className="text-2xl font-bold text-primary">€{totalEarnings.toFixed(2)}</p>
          </div>
        </div>
      </CardHeader>
      <CardContent className="pl-2">
        <div className="h-[350px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={chartData} margin={{ top: 10, right: 10, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="earningsGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="var(--primary)" stopOpacity={0.9} />
                  <stop offset="100%" stopColor="var(--primary)" stopOpacity={0.6} />
                </linearGradient>
              </defs>
              <CartesianGrid
                strokeDasharray="3 3"
                vertical={false}
                stroke="var(--border)"
                opacity={0.3}
              />
              <XAxis
                dataKey="name"
                stroke="var(--muted-foreground)"
                fontSize={12}
                tickLine={false}
                axisLine={false}
                dy={10}
              />
              <YAxis
                stroke="var(--muted-foreground)"
                fontSize={12}
                tickLine={false}
                axisLine={false}
                tickFormatter={(value) => `€${value}`}
                width={60}
              />
              <Tooltip
                content={<CustomTooltip />}
                cursor={{ fill: 'var(--primary)', opacity: 0.1, radius: 4 }}
              />
              <Bar
                dataKey="amount"
                fill="url(#earningsGradient)"
                radius={[8, 8, 0, 0]}
                maxBarSize={60}
              >
                {chartData.map((entry, index) => (
                  <Cell
                    key={`cell-${index}`}
                    className="hover:opacity-80 transition-opacity cursor-pointer"
                  />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
