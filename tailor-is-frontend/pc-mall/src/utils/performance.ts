/* eslint-disable no-unused-vars */
export function debounce<T extends (...args: unknown[]) => unknown>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null;
  return (...args: Parameters<T>) => {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(() => func(...args), wait);
  };
}

export function throttle<T extends (...args: unknown[]) => unknown>(
  func: T,
  limit: number
): (...args: Parameters<T>) => void {
  let inThrottle = false;
  return (...args: Parameters<T>) => {
    if (!inThrottle) {
      func(...args);
      inThrottle = true;
      setTimeout(() => (inThrottle = false), limit);
    }
  };
}

export function memoize<T extends (...args: unknown[]) => unknown>(
  func: T
): (...args: Parameters<T>) => T extends (...args: unknown[]) => infer R ? R : unknown {
  const cache = new Map<string, unknown>();
  return ((...args: Parameters<T>) => {
    const key = JSON.stringify(args);
    if (cache.has(key)) {
      return cache.get(key)!;
    }
    const result = func(...args);
    cache.set(key, result);
    return result;
  }) as (...args: Parameters<T>) => T extends (...args: unknown[]) => infer R ? R : unknown;
}

export function lazyLoadImage(img: HTMLImageElement, src: string): void {
  const imgSrc = img.getAttribute('data-src') || src;
  if (!imgSrc || img.src === imgSrc) return;

  const image = new Image();
  image.onload = () => {
    img.src = imgSrc;
    img.removeAttribute('data-src');
  };
  image.onerror = () => {
    img.src = '';
  };
  image.src = imgSrc;
}

export function observeIntersection(
  target: Element,
  callback: (_entry: IntersectionObserverEntry) => void,
  options: IntersectionObserverInit = {}
): IntersectionObserver {
  const observer = new IntersectionObserver(
    (entries: IntersectionObserverEntry[], _observer: IntersectionObserver) => {
      entries.forEach((_entry) => {
        if (_entry.isIntersecting) {
          callback(_entry);
        }
      });
    },
    {
      root: null,
      rootMargin: '0px',
      threshold: 0.1,
      ...options,
    }
  );

  observer.observe(target);
  return observer;
}

export function preloadImages(urls: string[]): void {
  urls.forEach((url) => {
    const img = new Image();
    img.src = url;
  });
}

export function prefetchResource(url: string, as: string = 'script'): void {
  const link = document.createElement('link');
  link.rel = 'prefetch';
  link.href = url;
  link.as = as;
  document.head.appendChild(link);
}

export function preloadResource(url: string, as: string = 'script'): void {
  const link = document.createElement('link');
  link.rel = 'preload';
  link.href = url;
  link.as = as;
  document.head.appendChild(link);
}

export function measurePerformance(): Promise<PerformanceEntry[]> {
  return new Promise((resolve) => {
    const observer = new PerformanceObserver((list) => {
      const entries = list.getEntries();
      observer.disconnect();
      resolve(entries);
    });

    observer.observe({ type: 'navigation', buffered: true });
  });
}

export function getPerformanceMetrics(): Record<string, number> {
  const nav = performance.getEntriesByType('navigation')[0] as PerformanceNavigationTiming;
  if (!nav) return {};

  return {
    dnsTime: nav.domainLookupEnd - nav.domainLookupStart,
    tcpTime: nav.connectEnd - nav.connectStart,
    tlsTime: nav.secureConnectionStart ? nav.connectEnd - nav.secureConnectionStart : 0,
    ttfb: nav.responseStart - nav.requestStart,
    fcp: nav.responseEnd - nav.requestStart,
    domContentLoaded: nav.domContentLoadedEventEnd - nav.requestStart,
    load: nav.loadEventEnd - nav.requestStart,
  };
}

export function logPerformanceMetrics(): void {
  const metrics = getPerformanceMetrics();
  if (Object.keys(metrics).length > 0) {
    console.info('📊 Performance Metrics:', metrics);
  }
}

export function setLazyLoadAttribute(): void {
  const images = document.querySelectorAll('img[data-src]');
  images.forEach((img) => {
    observeIntersection(img as Element, (entry) => {
      const target = entry.target as HTMLImageElement;
      const src = target.getAttribute('data-src');
      if (src) {
        lazyLoadImage(target, src);
      }
    });
  });
}

export function optimizeScrollPerformance(
  target: HTMLElement,
  onScroll: () => void,
  debounceDelay: number = 150
): void {
  let ticking = false;

  const update = () => {
    onScroll();
    ticking = false;
  };

  const requestTick = () => {
    if (!ticking) {
      window.requestAnimationFrame(update);
      ticking = true;
    }
  };

  target.addEventListener('scroll', debounce(requestTick, debounceDelay), { passive: true });
}

export function reduceLayoutThrashing(): void {
  const originalGetBoundingClientRect = Element.prototype.getBoundingClientRect;

  Element.prototype.getBoundingClientRect = function () {
    const result = originalGetBoundingClientRect.call(this);
    return result;
  };
}

export function optimizeImageLoading(): void {
  const images = document.querySelectorAll('img');
  images.forEach((img) => {
    const el = img as HTMLImageElement;
    if (!el.src && el.dataset.src) {
      el.loading = 'lazy';
    }
  });
}

export function initPerformanceOptimizations(): void {
  optimizeImageLoading();
  setLazyLoadAttribute();
  logPerformanceMetrics();
}

interface PerformanceMemory {
  usedJSHeapSize: number;
  totalJSHeapSize: number;
  jsHeapSizeLimit: number;
}

export function getMemoryUsage(): PerformanceMemory | null {
  const perf = performance as unknown as { memory?: PerformanceMemory };
  if (perf && perf.memory) {
    return perf.memory;
  }
  return null;
}

export function monitorMemoryUsage(threshold: number = 80): void {
  const checkMemory = () => {
    const memory = getMemoryUsage();
    if (memory) {
      const usagePercent = (memory.usedJSHeapSize / memory.totalJSHeapSize) * 100;
      if (usagePercent > threshold) {
        console.warn('⚠️ Memory usage high:', usagePercent.toFixed(2) + '%');
      }
    }
  };

  setInterval(checkMemory, 60000);
}
