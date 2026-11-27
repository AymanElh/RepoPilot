import {HttpErrorResponse} from '@angular/common/http';
import {Component, computed, DestroyRef, inject, signal} from '@angular/core';
import {ActivatedRoute, RouterLink} from '@angular/router';
import {RepositoriesApi} from '../../core/api/repositories.api';
import {ChatApi} from '../../core/api/chat.api';
import {ChatResponse, CodeSearchResult, ProvidersResponse, Repository} from '../../core/api/api.models';
import {ProvidersApi} from '../../core/api/providers.api';
import {takeUntilDestroyed} from '@angular/core/rxjs-interop';
import MarkdownIt from 'markdown-it';
import {finalize} from 'rxjs';
import {SearchApi} from '../../core/api/search.api';

const markdown = new MarkdownIt({html: false});

@Component({
  selector: 'app-repository-chat-page',
  imports: [RouterLink],
  templateUrl: './repository-chat-page.html',
  styleUrl: './repository-chat-page.css',
})
export class RepositoryChatPage {

  private readonly route = inject(ActivatedRoute);
  private readonly repositoriesApi = inject(RepositoriesApi);
  private readonly chatApi = inject(ChatApi);
  private readonly providersApi = inject(ProvidersApi);
  private readonly destroyRef = inject(DestroyRef);
  private readonly searchApi = inject(SearchApi);

  private readonly owner = this.route.snapshot.paramMap.get('owner');
  private readonly name = this.route.snapshot.paramMap.get('name');

  protected readonly repository = signal<Repository | null>(null)
  protected readonly response = signal<ChatResponse | null>(null)
  protected readonly answerHtml = computed(() => markdown.render(this.response()?.answer ?? ''));
  protected readonly loadingRepository = signal(false);
  protected readonly providers = signal<ProvidersResponse | null>(null);
  protected readonly loadingProviders = signal(true);
  protected readonly asking = signal(false);
  protected readonly error = signal('');

  protected readonly canAsk = computed(
    () => this.repository()?.status === 'READY' && !this.loadingProviders() && !this.asking()
  );

  protected readonly canSearch = computed(
    () => this.repository()?.status === 'READY' && !this.searching()
  );

  protected readonly searchResults = signal<CodeSearchResult[] | null>(null)
  protected readonly searching = signal(false);
  protected readonly searchError = signal('');


  constructor() {
    if (!this.owner || !this.name) {
      this.error.set('The repository URL is missing its owner or name');
      return;
    }

    this.loadingRepository.set(true);

    this.providersApi.list()
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingProviders.set(false))
      )
      .subscribe({
        next: (providers) => this.providers.set(providers),
        error: () => this.providers.set(null)
      });

    this.repositoriesApi.get(this.owner, this.name)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.loadingRepository.set(false))
      )
      .subscribe({
        next: (repo) => this.repository.set(repo),
        error: () => this.error.set('Could not load the repository')
      });
  }

  protected ask(question: string, selectedProvider: string): void {
    const trimmedQuestion = question.trim();

    if (!this.owner || !this.name || !trimmedQuestion || !this.canAsk) {
      return;
    }

    this.error.set('');
    this.response.set(null);
    this.asking.set(true);

    const provider = this.providers()?.providers.find((option) => option === selectedProvider);
    this.chatApi.ask(this.owner, this.name, trimmedQuestion, provider)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.asking.set(false)),
    )
      .subscribe({
        next: (resp) => this.response.set(resp),
        error: (error: unknown) => {
          const message = error instanceof HttpErrorResponse ? error.error?.message : undefined;
          this.error.set(typeof message === 'string' && message ? message : 'Could not get an answer. Try again.');
        }
      })
  }

  protected search(query: string): void {
    const trimmedQuery = query.trim();

    if (!this.owner || !this.name || !trimmedQuery || !this.canSearch()) {
      return;
    }

    this.searchError.set('');
    this.searchResults.set(null);
    this.searching.set(true);

    this.searchApi.search(this.owner, this.name, trimmedQuery)
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => this.searching.set(false))
      )
      .subscribe({
        next: (results) => this.searchResults.set(results),
        error: () => this.searchError.set('Could not search the repository, Try again')
      })
  }
}
