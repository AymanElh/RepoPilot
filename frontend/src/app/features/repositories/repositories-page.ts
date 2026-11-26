import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormField, FormRoot, form, pattern, required } from '@angular/forms/signals';
import { EMPTY, Subject, catchError, firstValueFrom, repeat, startWith, switchMap, timer } from 'rxjs';
import { Repository, RepositoryStatus } from '../../core/api/api.models';
import { RepositoriesApi } from '../../core/api/repositories.api';

const POLL_INTERVAL_MS = 3000;
const GITHUB_REPOSITORY_URL = /^https:\/\/github\.com\/[^/?#]+\/[^/?#]+$/i;

@Component({
  selector: 'app-repositories-page',
  imports: [FormField, FormRoot],
  templateUrl: './repositories-page.html',
  styleUrl: './repositories-page.css',
})
export class RepositoriesPage {
  private readonly api = inject(RepositoriesApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly reload = new Subject<void>();
  private readonly input = signal({ url: '' });

  protected readonly repositories = signal<Repository[] | null>(null);
  protected readonly listError = signal('');
  protected readonly submitError = signal('');
  protected readonly statusLabels: Record<RepositoryStatus, string> = {
    PENDING: 'Pending',
    INDEXING: 'Indexing',
    READY: 'Ready',
    FAILED: 'Failed',
  };
  private readonly hasActiveIndexing = computed(() =>
    this.repositories()?.some(({ status }) => status === 'PENDING' || status === 'INDEXING'),
  );

  protected readonly repositoryForm = form(
    this.input,
    ({ url }) => {
      required(url, { message: 'Enter a GitHub repository URL.' });
      pattern(url, GITHUB_REPOSITORY_URL, { message: 'Use a URL like https://github.com/owner/repo.' });
    },
    { submission: { action: () => this.indexRepository() } },
  );

  constructor() {
    this.reload
      .pipe(
        startWith(undefined),
        switchMap(() =>
          this.api.list().pipe(
            catchError((error: unknown) => {
              this.listError.set(errorMessage(error, 'Make sure the backend is running, then try again.'));
              return EMPTY;
            }),
            repeat({ delay: () => (this.hasActiveIndexing() ? timer(POLL_INTERVAL_MS) : EMPTY) }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((repositories) => {
        this.repositories.set(repositories);
        this.listError.set('');
      });
  }

  protected refresh(): void {
    this.listError.set('');
    this.reload.next();
  }

  private async indexRepository(): Promise<undefined> {
    this.submitError.set('');

    try {
      const repository = await firstValueFrom(
        this.api.index(this.input().url.trim()).pipe(takeUntilDestroyed(this.destroyRef)),
        { defaultValue: null },
      );

      if (repository) {
        this.repositories.update((current) => [
          repository,
          ...(current ?? []).filter(({ id }) => id !== repository.id),
        ]);
        this.repositoryForm().reset({ url: '' });
        this.refresh();
      }
    } catch (error) {
      this.submitError.set(errorMessage(error, 'Could not start indexing. Try again.'));
      // The backend can save a failed repository before returning an error.
      this.refresh();
    }

    return undefined;
  }
}

function errorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpErrorResponse && typeof error.error?.message === 'string') {
    return error.error.message || fallback;
  }

  return fallback;
}
