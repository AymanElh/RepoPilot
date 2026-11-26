import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { vi } from 'vitest';
import { App } from './app';
import { appConfig } from './app.config';
import { Repository } from './core/api/api.models';

const repository: Repository = {
  id: '1',
  owner: 'spring-projects',
  fullName: 'spring-projects/spring-petclinic',
  defaultBranch: 'main',
  indexedCommitSha: null,
  status: 'INDEXING',
  errorMessage: null,
  javaFiles: 0,
  chunkCount: 0,
  createdAt: '2026-11-26T00:00:00Z',
  updatedAt: '2026-11-26T00:00:00Z',
};

describe('App', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [App],
      providers: [...appConfig.providers, provideHttpClientTesting()],
    });
  });

  afterEach(() => {
    TestBed.inject(HttpTestingController).verify();
    vi.useRealTimers();
  });

  async function openRepositories() {
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();
    const router = TestBed.inject(Router);
    await router.navigateByUrl('/');
    await fixture.whenStable();
    expect(router.url).toBe('/repositories');
    return fixture;
  }

  it('opens the repository library and displays its empty state', async () => {
    const fixture = await openRepositories();
    TestBed.inject(HttpTestingController).expectOne('/api/repositories').flush([]);
    fixture.detectChanges();
    const page: HTMLElement = fixture.nativeElement;
    expect(page.querySelector('h1')?.textContent).toBe('Repositories');
    expect(page.textContent).toContain('No repositories yet');
  });

  it('submits a URL and stops polling when the repository is ready', async () => {
    vi.useFakeTimers({ toFake: ['setInterval', 'clearInterval'] });
    const fixture = await openRepositories();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne({ method: 'GET', url: '/api/repositories' }).flush([]);
    fixture.detectChanges();

    const page: HTMLElement = fixture.nativeElement;
    const input = page.querySelector('input')!;
    const url = `https://github.com/${repository.fullName}`;
    input.value = url;
    input.dispatchEvent(new Event('input'));
    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));
    await Promise.resolve();

    const request = http.expectOne({ method: 'POST', url: '/api/repositories' });
    expect(request.request.body).toEqual({ url });
    request.flush(repository, { status: 202, statusText: 'Accepted' });
    await Promise.resolve();
    http.expectOne({ method: 'GET', url: '/api/repositories' }).flush([repository]);
    fixture.detectChanges();
    expect(page.querySelector('.status')?.textContent).toContain('Indexing');

    vi.advanceTimersByTime(3000);
    http.expectOne({ method: 'GET', url: '/api/repositories' }).flush([
      { ...repository, status: 'READY', javaFiles: 12, chunkCount: 24 },
    ]);
    fixture.detectChanges();
    expect(page.querySelector('.status')?.textContent).toContain('Ready');
    expect(page.textContent).toContain('12 Java files · 24 chunks');
    vi.advanceTimersByTime(6000);
    http.expectNone('/api/repositories');
  });

  it('recovers from a list error using Try again', async () => {
    const fixture = await openRepositories();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/repositories').flush('', { status: 502, statusText: 'Bad Gateway' });
    fixture.detectChanges();
    const page: HTMLElement = fixture.nativeElement;
    expect(page.querySelector('[role=alert]')?.textContent).toContain('Couldn’t load repositories');
    page.querySelector<HTMLButtonElement>('button[type=button]')!.click();
    http.expectOne('/api/repositories').flush([]);
    fixture.detectChanges();
    expect(page.textContent).toContain('No repositories yet');
  });

  it('renders a Markdown answer and its source from the selected provider', async () => {
    const fixture = await openRepositories();
    const http = TestBed.inject(HttpTestingController);
    const readyRepository: Repository = { ...repository, status: 'READY' };
    http.expectOne('/api/repositories').flush([readyRepository]);
    fixture.detectChanges();

    const page: HTMLElement = fixture.nativeElement;
    const repositoryPath = `/repositories/${repository.fullName}`;
    const chatLink = page.querySelector<HTMLAnchorElement>('a.chat-link')!;
    expect(chatLink.getAttribute('href')).toBe(repositoryPath);
    chatLink.click();
    await fixture.whenStable();

    http.expectOne({ method: 'GET', url: `/api${repositoryPath}` }).flush(readyRepository);
    http.expectOne('/api/ai/providers').flush({ defaultProvider: 'GEMINI', providers: ['OLLAMA', 'GEMINI'] });
    fixture.detectChanges();
    const provider = page.querySelector<HTMLSelectElement>('#chat-provider')!;
    expect(provider.value).toBe('GEMINI');
    provider.value = 'OLLAMA';
    page.querySelector('textarea')!.value = 'Where is the application configured?';
    page.querySelector('form')!.dispatchEvent(new Event('submit', { cancelable: true }));

    const request = http.expectOne({ method: 'POST', url: `/api${repositoryPath}/chat` });
    expect(request.request.body).toEqual({ question: 'Where is the application configured?', provider: 'OLLAMA' });
    fixture.detectChanges();
    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(true);

    const githubUrl = `https://github.com/${repository.fullName}/blob/main/src/App.java#L4-L8`;
    request.flush({
      answer: [
        '### Entry point',
        '',
        'The application starts in `App.java` [SOURCE 1].',
        '',
        '- Load the **configuration**.',
        '',
        '```java',
        'class App {}',
        '```',
        '',
        '<b id="raw-markup">Untrusted HTML</b>',
      ].join('\n'),
      sources: [{ filePath: 'src/App.java', startLine: 4, endLine: 8, githubUrl }],
    });
    fixture.detectChanges();
    const answer = page.querySelector('.chat-answer-markdown');
    expect(answer?.querySelector('h3')?.textContent).toBe('Entry point');
    expect(answer?.querySelector('p code')?.textContent).toBe('App.java');
    expect(answer?.querySelector('li strong')?.textContent).toBe('configuration');
    expect(answer?.querySelector('pre code')?.textContent).toBe('class App {}\n');
    expect(answer?.querySelector('#raw-markup')).toBeNull();
    expect(page.querySelector('.sources a')?.getAttribute('href')).toBe(githubUrl);
    expect(page.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBe(false);
  });
});
