import { useParams, useNavigate } from 'react-router-dom';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Camera, Lock, AlertCircle } from 'lucide-react';
import { galleryApi } from '../../api/endpoints';
import { PinVerifyResponse } from '../../types';
import { useState } from 'react';
import toast from 'react-hot-toast';

const pinSchema = z.object({
  pin: z.string().length(6, 'PIN must be exactly 6 digits').regex(/^\d{6}$/, 'PIN must be 6 digits'),
});

type PinForm = z.infer<typeof pinSchema>;

export function PublicGalleryPage() {
  const { token } = useParams<{ token: string }>();
  const navigate = useNavigate();
  const [showError, setShowError] = useState(false);
  const [isVerifying, setIsVerifying] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<PinForm>({
    resolver: zodResolver(pinSchema),
  });

  const { data: galleryInfo, isLoading, error } = useQuery({
    queryKey: ['public-gallery', token],
    queryFn: () => galleryApi.getPublicInfo(token!),
    enabled: !!token,
    retry: false,
  });

  const verifyMutation = useMutation({
    mutationFn: (data: PinForm) => galleryApi.verifyPin(token!, data),
    onSuccess: (response: PinVerifyResponse) => {
      const accessToken = response.accessToken;
      sessionStorage.setItem(`gallery_token_${token}`, accessToken);
      navigate(`/gallery/${token}/photos`);
    },
    onError: () => {
      setShowError(true);
      toast.error('Invalid PIN');
      setTimeout(() => setShowError(false), 3000);
    },
  });

  const onSubmit = (data: PinForm) => {
    setIsVerifying(true);
    verifyMutation.mutate(data, {
      onSettled: () => setIsVerifying(false),
    });
  };

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="max-w-md w-full">
          <div className="bg-white rounded-2xl shadow-xl p-8 text-center">
            <div className="animate-spin rounded-full h-12 w-12 border-4 border-indigo-600 border-t-transparent mx-auto mb-4"></div>
            <p className="text-gray-600">Loading gallery...</p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !galleryInfo) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4">
        <div className="max-w-md w-full">
          <div className="bg-white rounded-2xl shadow-xl p-8 text-center">
            <AlertCircle className="h-16 w-16 text-red-400 mx-auto mb-4" />
            <h2 className="text-xl font-semibold text-gray-900 mb-2">Gallery Not Found</h2>
            <p className="text-gray-600 mb-6">This gallery doesn't exist or has been unpublished.</p>
            <button
              onClick={() => window.history.back()}
              className="btn-primary"
            >
              Go Back
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 px-4 py-12">
      <div className="max-w-md w-full">
        <div className="bg-white rounded-2xl shadow-xl overflow-hidden">
          <div className="bg-gradient-to-r from-indigo-600 to-purple-600 px-8 py-12 text-center">
            <Camera className="h-16 w-16 text-white mx-auto mb-4" />
            <h1 className="text-2xl font-bold text-white mb-2">{galleryInfo.eventName}</h1>
            <p className="text-indigo-100">{galleryInfo.photoCount} photos</p>
          </div>

          <div className="p-8">
            <div className="flex items-center justify-center gap-3 mb-8">
              <Lock className="h-10 w-10 text-indigo-600 bg-indigo-50 rounded-full flex items-center justify-center" />
              <div>
                <p className="text-sm text-gray-500">Private Gallery</p>
                <p className="font-medium text-gray-900">Enter PIN to access</p>
              </div>
            </div>

            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div className="relative">
                <input
                  type="password"
                  {...register('pin')}
                  className="input text-center tracking-widest text-3xl py-4"
                  placeholder="••••••"
                  maxLength={6}
                  disabled={isVerifying}
                  autoComplete="one-time-code"
                />
                {showError && (
                  <div className="absolute inset-0 border-2 border-red-500 rounded-lg pointer-events-none" />
                )}
              </div>
              {errors.pin && <p className="text-sm text-red-600 text-center">{errors.pin.message}</p>}

              <button
                type="submit"
                disabled={isVerifying}
                className="btn-primary w-full py-4 text-lg"
              >
                {isVerifying ? (
                  <span className="flex items-center justify-center gap-2">
                    <span className="animate-spin rounded-full h-5 w-5 border-2 border-white border-t-transparent"></span>
                    Verifying...
                  </span>
                ) : (
                  'Access Gallery'
                )}
              </button>
            </form>

            <p className="text-center text-sm text-gray-500 mt-6">
              Don't have a PIN? Contact the event organizer.
            </p>
          </div>
        </div>
      </div>
    </div>
  );
}