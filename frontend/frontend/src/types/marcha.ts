export type BandType = "BANDA_PALIO" | "AGRUPACION_MUSICAL" | "BANDA_CORNETAS";

export interface Marcha {
  id: string;
  title: string;
  composer: string;
  bandType: BandType;
  durationSeconds: number;
  compositionYear: number | null;
  youtubeUrl: string | null;
  createdAt: string;
  updatedAt: string;
}
