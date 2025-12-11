import axios from 'axios';
import { API_BASE_URL } from '@/lib/api';

const API_URL = `${API_BASE_URL}/api/tools`;

// Tool type definition
export interface Tool {
  id: string;
  title: string;
  description: string;
  pricePerDay: number;
  location: string;
  active: boolean;
  availabilityCalendar?: string;
  overallRating: number;
  numRatings: number;
}

export interface ToolOwnerSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  reputationScore: number;
}

export interface ToolDetails extends Tool {
  owner: ToolOwnerSummary;
}

// CreateToolInput - matches backend DTO
export interface CreateToolInput {
  title: string;
  description: string;
  pricePerDay: number;
  supplierId: string;
  location: string;
}

// UpdateToolInput - matches backend DTO
export interface UpdateToolInput {
  title?: string;
  description?: string;
  pricePerDay?: number;
  location?: string;
  ownerId?: string;
  active?: boolean;
  availabilityCalendar?: string;
  overallRating?: number;
  numRatings?: number;
}

// Get all tools
export const getAllTools = async (): Promise<Tool[]> => {
  try {
    const response = await axios.get<Tool[]>(API_URL);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch tools');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Get tool by ID
export const getToolById = async (toolId: string): Promise<ToolDetails> => {
  try {
    const response = await axios.get<ToolDetails>(`${API_URL}/${toolId}`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch tool');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Explicit tool details accessor (alias for readability)
export const getToolDetails = async (toolId: string): Promise<ToolDetails> => {
  try {
    const response = await axios.get<ToolDetails>(`${API_URL}/${toolId}`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch tool');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Search tools
export const searchTools = async (
  keyword?: string, 
  location?: string,
  minPrice?: number,
  maxPrice?: number
): Promise<Tool[]> => {
  try {
    const params = new URLSearchParams();
    if (keyword) params.append('keyword', keyword);
    if (location) params.append('location', location);
    if (minPrice !== undefined) params.append('minPrice', minPrice.toString());
    if (maxPrice !== undefined) params.append('maxPrice', maxPrice.toString());
    
    const response = await axios.get<Tool[]>(`${API_URL}/search`, { params });
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to search tools');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Get only active tools
export const getActiveTools = async (): Promise<Tool[]> => {
  try {
    const response = await axios.get<Tool[]>(`${API_URL}/active`);
    return response.data;
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to fetch active tools');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Create a new tool
export const createTool = async (input: CreateToolInput): Promise<void> => {
  try {
    await axios.post(API_URL, input);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to create tool');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Update a tool
export const updateTool = async (toolId: string, input: UpdateToolInput): Promise<void> => {
  try {
    await axios.put(`${API_URL}/${toolId}`, input);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to update tool');
    }
    throw new Error('Network error or unknown issue');
  }
};

// Delete a tool
export const deleteTool = async (toolId: string): Promise<void> => {
  try {
    await axios.delete(`${API_URL}/${toolId}`);
  } catch (error) {
    if (axios.isAxiosError(error) && error.response) {
      throw new Error(error.response.data.message || 'Failed to delete tool');
    }
    throw new Error('Network error or unknown issue');
  }
};
