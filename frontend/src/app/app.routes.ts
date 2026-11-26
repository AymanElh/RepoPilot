import { Routes } from '@angular/router';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'repositories' },
  {
    path: 'repositories',
    title: 'Repositories · RepoPilot',
    loadComponent: () =>
      import('./features/repositories/repositories-page').then((module) => module.RepositoriesPage),
  },
];
