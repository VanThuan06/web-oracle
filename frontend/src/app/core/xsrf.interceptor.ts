import { HttpInterceptorFn } from '@angular/common/http';

export function getCookie(name: string): string | null {
  if (typeof document === 'undefined') return null;
  const match = document.cookie.match(new RegExp('(?:^|;\\s*)' + name.replace(/([.*+?^=!:${}()|[\]/\\])/g, '\\$1') + '=([^;]*)'));
  return match ? decodeURIComponent(match[1]) : null;
}

export const xsrfInterceptor: HttpInterceptorFn = (req, next) => {
  const mutatingMethods = ['POST', 'PUT', 'PATCH', 'DELETE'];
  if (mutatingMethods.includes(req.method.toUpperCase())) {
    const token = getCookie('XSRF-TOKEN');
    if (token && !req.headers.has('X-XSRF-TOKEN')) {
      const cloned = req.clone({
        setHeaders: {
          'X-XSRF-TOKEN': token,
        },
        withCredentials: true,
      });
      return next(cloned);
    }
  }
  return next(req);
};
