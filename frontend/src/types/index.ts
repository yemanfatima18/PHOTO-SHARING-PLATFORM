export interface User {
  id: string;
  name: string;
  email: string;
  role: 'ADMIN' | 'TEAM_MEMBER';
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: User;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

export interface Event {
  id: string;
  name: string;
  description?: string;
  eventDate: string;
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  photoCount: number;
  selectedPhotoCount: number;
  hasGallery: boolean;
  galleryPublished: boolean;
}

export interface EventRequest {
  name: string;
  description?: string;
  eventDate: string;
}

export interface EventMember {
  id: string;
  userId: string;
  userName: string;
  userEmail: string;
  userRole: 'ADMIN' | 'TEAM_MEMBER';
  assignedAt: string;
}

export interface EventMemberRequest {
  userId: string;
}

export interface Photo {
  id: string;
  eventId: string;
  uploadedById: string;
  uploadedByName: string;
  originalFilename: string;
  storageKey: string;
  fileSize: number;
  contentType: string;
  selectedForPublishing: boolean;
  createdAt: string;
  signedUrl?: string;
}

export interface PhotoUploadUrlRequest {
  filename: string;
  contentType: string;
  fileSize: number;
}

export interface PhotoCompleteRequest {
  etag: string;
}

export interface PhotoSelectionRequest {
  photoIds: string[];
  selected: boolean;
}

export interface PhotoUploadUrlResponse {
  photoId: string;
  uploadUrl: string;
  storageKey: string;
  expiresIn: number;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}

export interface Gallery {
  id: string;
  eventId: string;
  eventName: string;
  publicToken: string;
  published: boolean;
  publishedAt?: string;
  createdAt: string;
  updatedAt: string;
  photoCount: number;
  shareUrl?: string;
}

export interface GalleryCreateRequest {
  pin: string;
}

export interface PinVerifyRequest {
  pin: string;
}

export interface PublicGalleryInfo {
  eventName: string;
  eventDate: string;
  galleryToken: string;
  requiresPin: boolean;
  photoCount: number;
}

export interface PublicGalleryPhoto {
  id: string;
  originalFilename: string;
  contentType: string;
  fileSize: number;
  signedUrl: string;
  displayOrder: number;
}

export interface PinVerifyResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  galleryToken: string;
}

export interface ErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors?: Record<string, string>;
}