import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { registerLocaleData } from '@angular/common';
import localeVi from '@angular/common/locales/vi';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { xsrfInterceptor } from './app/core/xsrf.interceptor';

registerLocaleData(localeVi);

bootstrapApplication(AppComponent, {
  providers: [
    provideRouter(routes),
    provideHttpClient(
      withXsrfConfiguration({
        cookieName: 'XSRF-TOKEN',
        headerName: 'X-XSRF-TOKEN',
      }),
      withInterceptors([xsrfInterceptor])
    ),
  ],
}).catch((error: unknown) => console.error('Unable to start NewMen Store', error));
