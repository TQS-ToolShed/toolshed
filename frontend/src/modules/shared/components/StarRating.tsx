import React from "react";
import { Star, StarHalf } from "lucide-react";

interface StarRatingProps {
  rating: number; // 0 to 5
  size?: number;
  showCount?: boolean;
  count?: number;
  className?: string;
}

const StarRating: React.FC<StarRatingProps> = ({
  rating,
  size = 16,
  showCount = false,
  count = 0,
  className = "",
}) => {
  // Clamp rating between 0 and 5
  const clampedRating = Math.min(Math.max(rating, 0), 5);

  // Calculate full stars
  const fullStars = Math.floor(clampedRating);
  const hasHalfStar = clampedRating % 1 >= 0.5;
  const emptyStars = 5 - fullStars - (hasHalfStar ? 1 : 0);

  return (
    <div className={`flex items-center gap-1 ${className}`}>
      <div className="flex text-yellow-500">
        {[...Array(fullStars)].map((_, i) => (
          <Star
            key={`full-${i}`}
            size={size}
            fill="currentColor"
            strokeWidth={0}
          />
        ))}
        {hasHalfStar && (
          <StarHalf size={size} fill="currentColor" strokeWidth={0} />
        )}
        {[...Array(emptyStars)].map((_, i) => (
          <Star
            key={`empty-${i}`}
            size={size}
            className="text-gray-300"
            strokeWidth={0}
            fill="currentColor"
          />
        ))}
      </div>
      {showCount && (
        <span className="text-sm text-gray-500 ml-1">
          ({count} {count === 1 ? "review" : "reviews"})
        </span>
      )}
    </div>
  );
};

export default StarRating;
