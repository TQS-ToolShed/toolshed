import { Button } from '@/components/ui/button';
import { useNavigate } from 'react-router-dom';

export const BackToDashboardButton = () => {
  const navigate = useNavigate();

  return (
    <Button variant="outline" onClick={() => navigate('/renter')}>
      Back to Dashboard
    </Button>
  );
};