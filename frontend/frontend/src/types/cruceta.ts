export interface CrucetaItem {
  id: string;
  marchaId: string;
  orderIndex: number;
  notes: string | null;
}

export interface Cruceta {
  id: string;
  procesionId: string;
  items: CrucetaItem[];
  createdAt: string;
  updatedAt: string;
}

export interface CrucetaRequest {
  items: { marchaId: string; orderIndex: number; notes: string | null }[];
}
