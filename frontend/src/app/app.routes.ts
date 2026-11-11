import { Routes } from '@angular/router';
import { AccountComponent } from './pages/account.component';
import { AdminComponent } from './pages/admin.component';
import { CartComponent } from './pages/cart.component';
import { ProductComponent } from './pages/product.component';
import { StoreComponent } from './pages/store.component';

export const routes: Routes = [
  { path: '', component: StoreComponent },
  { path: 'catalog', component: StoreComponent },
  { path: 'product/:slug', component: ProductComponent },
  { path: 'cart', component: CartComponent },
  { path: 'account', component: AccountComponent },
  { path: 'admin', component: AdminComponent },
  { path: '**', redirectTo: '' },
];
