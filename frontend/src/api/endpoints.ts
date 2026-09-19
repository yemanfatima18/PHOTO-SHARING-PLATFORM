import api, { handleApiError } from './axios';
import {
  AuthResponse,
  LoginRequest,
  RegisterRequest,
  User,
  Event,
  EventRequest,
  EventMember,
  EventMemberRequest,
  PageResponse,
  Photo,
  PhotoSelectionRequest,
  Gallery,
  GalleryCreateRequest,
  PinVerifyRequest,
  PinVerifyResponse,
  PublicGalleryInfo,
  PublicGalleryPhoto,
} from '../types';

export const authApi = {
  register: (data: RegisterRequest) => api.post<AuthResponse>('/auth/register', data).then(r => r.data),
  login: (data: LoginRequest) => api.post<AuthResponse>('/auth/login', data).then(r => r.data),
  me: () => api.get<User>('/auth/me').then(r => r.data),
};

export const eventApi = {
  create: (data: EventRequest) => api.post<Event>('/events', data).then(r => r.data),
  getMyEvents: (page = 0, size = 20) => api.get<PageResponse<Event>>('/events', { params: { page, size } }).then(r => r.data),
  getMyEventsList: () => api.get<Event[]>('/events/list').then(r => r.data),
  getById: (id: string) => api.get<Event>(`/events/${id}`).then(r => r.data),
  addMember: (eventId: string, data: EventMemberRequest) => api.post<EventMember>(`/events/${eventId}/members`, data).then(r => r.data),
  getMembers: (eventId: string) => api.get<EventMember[]>(`/events/${eventId}/members`).then(r => r.data),
  removeMember: (eventId: string, memberId: string) => api.delete(`/events/${eventId}/members/${memberId}`).then(r => r.data),
};

export const photoApi = {
  upload: (eventId: string, file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return api.post<Photo>(`/events/${eventId}/photos`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    }).then(r => r.data);
  },
  getEventPhotos: (eventId: string, page = 0, size = 20) => api.get<PageResponse<Photo>>(`/events/${eventId}/photos`, { params: { page, size } }).then(r => r.data),
  getMyPhotos: (eventId: string, page = 0, size = 20) => api.get<PageResponse<Photo>>(`/events/${eventId}/photos/mine`, { params: { page, size } }).then(r => r.data),
  updateSelection: (eventId: string, data: PhotoSelectionRequest) => api.patch(`/events/${eventId}/photos/selection`, data).then(r => r.data),
  getSelectedPhotos: (eventId: string) => api.get<Photo[]>(`/events/${eventId}/photos/selected`).then(r => r.data),
  download: (eventId: string, photoId: string) => api.get(`/events/${eventId}/photos/${photoId}/download`, { responseType: 'blob' }).then(r => r.data),
};

export const galleryApi = {
  create: (eventId: string, data: GalleryCreateRequest) => api.post<Gallery>(`/events/${eventId}/gallery`, data).then(r => r.data),
  getGallery: (eventId: string) => api.get<Gallery>(`/events/${eventId}/gallery`).then(r => r.data),
  publish: (eventId: string) => api.post<Gallery>(`/events/${eventId}/gallery/publish`).then(r => r.data),
  unpublish: (eventId: string) => api.post(`/events/${eventId}/gallery/unpublish`).then(r => r.data),
  verifyPin: (token: string, data: PinVerifyRequest) => api.post<PinVerifyResponse>(`/public/galleries/${token}/verify`, data).then(r => r.data),
  getPublicInfo: (token: string) => api.get<PublicGalleryInfo>(`/public/galleries/${token}`).then(r => r.data),
  getPublicPhotos: (galleryToken: string, accessToken: string) => api.get<PublicGalleryPhoto[]>(`/public/galleries/${galleryToken}/photos`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  }).then(r => r.data),
  getPublicPhoto: (token: string, photoId: string, accessToken: string) => api.get(`/public/galleries/${token}/photos/${photoId}`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    responseType: 'blob',
  }).then(r => r.data),
};

export { handleApiError };