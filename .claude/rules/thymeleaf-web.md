# Java Web with Thymeleaf + Bootstrap 5 Standards

## Project Structure

### Web Layer Organization

```
src/main/java/com/company/project/
├── controller/
│ ├── web/ # Thymeleaf controllers
│ │ ├── PageController.java
│ │ ├── AuthController.java
│ │ └── AdminController.java
│ └── api/ # REST controllers (if needed)
├── service/
├── dto/
└── config/

src/main/resources/
├── templates/ # Thymeleaf templates
│ ├── fragments/ # Reusable fragments
│ │ ├── header.html
│ │ ├── footer.html
│ │ ├── navbar.html
│ │ └── modals.html
│ ├── layouts/ # Layout templates
│ │ ├── default.html
│ │ └── admin.html
│ ├── pages/
│ │ ├── home.html
│ │ ├── login.html
│ │ ├── register.html
│ │ └── dashboard.html
│ └── error/
│ ├── 404.html
│ └── 500.html
├── static/
│ ├── css/
│ │ ├── custom.css
│ │ └── components.css
│ ├── js/
│ │ ├── main.js
│ │ ├── components/
│ │ │ ├── navbar.js
│ │ │ ├── forms.js
│ │ │ └── modals.js
│ │ └── pages/
│ │ ├── home.js
│ │ ├── dashboard.js
│ │ └── admin.js
│ └── images/
└── application.yml
```

## Dependencies

### Maven pom.xml
```xml
<dependencies>
    <!-- Spring Boot Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    
    <!-- Thymeleaf -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-thymeleaf</artifactId>
    </dependency>
    
    <!-- Thymeleaf Layout Dialect -->
    <dependency>
        <groupId>nz.net.ultraq.thymeleaf</groupId>
        <artifactId>thymeleaf-layout-dialect</artifactId>
    </dependency>
    
    <!-- Thymeleaf Security (if using Spring Security) -->
    <dependency>
        <groupId>org.thymeleaf.extras</groupId>
        <artifactId>thymeleaf-extras-springsecurity6</artifactId>
    </dependency>
    
    <!-- Bootstrap 5 WebJars -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap</artifactId>
        <version>5.3.2</version>
    </dependency>
    
    <!-- Bootstrap Icons -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>bootstrap-icons</artifactId>
        <version>1.11.3</version>
    </dependency>
    
    <!-- Font Awesome (optional) -->
    <dependency>
        <groupId>org.webjars</groupId>
        <artifactId>font-awesome</artifactId>
        <version>6.5.1</version>
    </dependency>
    
    <!-- Validation -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
</dependencies>
```

Thymeleaf Configuration

application.yml
```yaml
spring:
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML
    encoding: UTF-8
    cache: false  # Set to true in production
    check-template-location: true
    
  web:
    resources:
      static-locations: classpath:/static/
      cache:
        period: 3600
      
  mvc:
    view:
      prefix: /templates/
      suffix: .html
    static-path-pattern: /static/**
```

Thymeleaf Config Class
```java
@Configuration
public class ThymeleafConfig {
    
    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
    
    @Bean
    public SpringSecurityDialect springSecurityDialect() {
        return new SpringSecurityDialect();
    }
    
    @Bean
    public ITemplateResolver templateResolver() {
        SpringResourceTemplateResolver resolver = new SpringResourceTemplateResolver();
        resolver.setPrefix("classpath:/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        return resolver;
    }
}
```

Layout Templates

fragments/header.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:fragment="header(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title}">My Application</title>
    
    <!-- Bootstrap CSS -->
    <link rel="stylesheet" 
          th:href="@{/webjars/bootstrap/5.3.2/css/bootstrap.min.css}">
    
    <!-- Bootstrap Icons -->
    <link rel="stylesheet" 
          th:href="@{/webjars/bootstrap-icons/1.11.3/font/bootstrap-icons.css}">
    
    <!-- Font Awesome (optional) -->
    <link rel="stylesheet" 
          th:href="@{/webjars/font-awesome/6.5.1/css/all.min.css}">
    
    <!-- Custom CSS -->
    <link rel="stylesheet" th:href="@{/static/css/custom.css}">
    
    <!-- Page-specific CSS -->
    <th:block th:if="${pageCss != null}">
        <link rel="stylesheet" th:href="@{${pageCss}}">
    </th:block>
</head>
```

layouts/default.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{fragments/header}">
<head>
    <title layout:title-pattern="$DECORATOR_TITLE - $CONTENT_TITLE">Default</title>
    <th:block layout:fragment="head-content"></th:block>
</head>
<body>
    <!-- Navbar -->
    <th:block th:replace="~{fragments/navbar :: navbar}"></th:block>
    
    <!-- Main Content -->
    <main class="container py-4" role="main">
        <!-- Breadcrumbs -->
        <th:block th:if="${breadcrumbs != null}" 
                  th:replace="~{fragments/breadcrumbs :: breadcrumbs(${breadcrumbs})}">
        </th:block>
        
        <!-- Page Content -->
        <div layout:fragment="content">
            <!-- Content from child templates -->
        </div>
    </main>
    
    <!-- Footer -->
    <th:block th:replace="~{fragments/footer :: footer}"></th:block>
    
    <!-- Modals -->
    <th:block th:replace="~{fragments/modals :: modals}"></th:block>
    
    <!-- Bootstrap JavaScript Bundle -->
    <script th:src="@{/webjars/bootstrap/5.3.2/js/bootstrap.bundle.min.js}"></script>
    
    <!-- Main JavaScript -->
    <script th:src="@{/static/js/main.js}"></script>
    
    <!-- Page-specific JavaScript -->
    <th:block th:if="${pageJs != null}">
        <script th:src="@{${pageJs}}"></script>
    </th:block>
    
    <!-- Additional Scripts -->
    <th:block layout:fragment="scripts"></th:block>
</body>
</html>
```

Controllers

Base Controller Pattern
```java
@Controller
public abstract class BaseWebController {
    
    protected Model addCommonAttributes(Model model) {
        model.addAttribute("appName", "My Application");
        model.addAttribute("version", "1.0.0");
        return model;
    }
    
    protected String renderPage(Model model, String page, String title) {
        addCommonAttributes(model);
        model.addAttribute("title", title);
        return page;
    }
}
```

Page Controller Example
```java
@Controller
@RequestMapping("/")
public class HomeController extends BaseWebController {
    
    @GetMapping
    public String home(Model model) {
        model.addAttribute("pageCss", "/static/css/pages/home.css");
        model.addAttribute("pageJs", "/static/js/pages/home.js");
        model.addAttribute("featuredProducts", productService.getFeatured());
        return renderPage(model, "pages/home", "Home");
    }
    
    @GetMapping("/dashboard")
    public String dashboard(Model model, Authentication authentication) {
        model.addAttribute("pageJs", "/static/js/pages/dashboard.js");
        model.addAttribute("user", userService.getCurrentUser(authentication));
        model.addAttribute("stats", dashboardService.getStats());
        return renderPage(model, "pages/dashboard", "Dashboard");
    }
}
```

Form Controller with Validation
```java
@Controller
@RequestMapping("/admin")
public class AdminController extends BaseWebController {
    
    @GetMapping("/products/create")
    public String showCreateForm(Model model) {
        model.addAttribute("product", new ProductDto());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("pageJs", "/static/js/pages/admin/products.js");
        return renderPage(model, "pages/admin/product-form", "Create Product");
    }
    
    @PostMapping("/products")
    public String createProduct(
        @Valid @ModelAttribute("product") ProductDto product,
        BindingResult result,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (result.hasErrors()) {
            model.addAttribute("categories", categoryService.findAll());
            return "pages/admin/product-form";
        }
        
        try {
            productService.create(product);
            redirectAttributes.addFlashAttribute("success", 
                "Product created successfully!");
            return "redirect:/admin/products";
        } catch (BusinessException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/admin/products/create";
        }
    }
}
```

Thymeleaf Templates Best Practices

Home Page Template
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/default}">
      
<head>
    <th:block layout:fragment="head-content">
        <meta name="description" content="Welcome to our store">
        <meta property="og:title" th:content="${title}">
    </th:block>
</head>

<body>
    <div layout:fragment="content">
        <!-- Hero Section -->
        <section class="hero-section bg-primary text-white py-5 mb-5">
            <div class="container">
                <div class="row align-items-center">
                    <div class="col-lg-6">
                        <h1 class="display-4 fw-bold">Welcome to Our Store</h1>
                        <p class="lead">Discover amazing products at great prices</p>
                        <a th:href="@{/products}" class="btn btn-light btn-lg">
                            Shop Now
                            <i class="bi bi-arrow-right"></i>
                        </a>
                    </div>
                </div>
            </div>
        </section>
        
        <!-- Featured Products -->
        <section class="featured-products">
            <h2 class="text-center mb-4">Featured Products</h2>
            <div class="row row-cols-1 row-cols-md-3 g-4">
                <div th:each="product : ${featuredProducts}" 
                     class="col">
                    <div class="card h-100 shadow-sm">
                        <img th:src="@{${product.imageUrl}}" 
                             class="card-img-top" 
                             th:alt="${product.name}"
                             style="height: 200px; object-fit: cover;">
                        <div class="card-body">
                            <h5 class="card-title" th:text="${product.name}">Product</h5>
                            <p class="card-text text-muted" 
                               th:text="${#strings.abbreviate(product.description, 100)}">
                            </p>
                            <div class="d-flex justify-content-between align-items-center">
                                <span class="h5 mb-0 text-primary" 
                                      th:text="${#numbers.formatDecimal(product.price, 0, 2, 'USD')}">
                                </span>
                                <span th:if="${product.inStock}" 
                                      class="badge bg-success">In Stock</span>
                                <span th:unless="${product.inStock}" 
                                      class="badge bg-danger">Out of Stock</span>
                            </div>
                        </div>
                        <div class="card-footer bg-transparent border-0">
                            <a th:href="@{/products/{id}(id=${product.id})}" 
                               class="btn btn-outline-primary w-100">
                                View Details
                            </a>
                        </div>
                    </div>
                </div>
            </div>
        </section>
        
        <!-- Alerts from Flash Attributes -->
        <div th:replace="~{fragments/alerts :: alerts}"></div>
    </div>
</body>
</html>
```

Form Template Example
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org"
      xmlns:layout="http://www.ultraq.net.nz/thymeleaf/layout"
      layout:decorate="~{layouts/default}">
      
<body>
    <div layout:fragment="content">
        <div class="row justify-content-center">
            <div class="col-lg-8">
                <div class="card shadow">
                    <div class="card-header bg-primary text-white">
                        <h4 class="mb-0" th:text="${product.id != null ? 'Edit Product' : 'Create Product'}">
                            Create Product
                        </h4>
                    </div>
                    <div class="card-body">
                        <form th:action="@{${product.id != null ? '/admin/products/' + product.id : '/admin/products'}}"
                              th:object="${product}" 
                              method="post" 
                              novalidate
                              id="productForm">
                              
                            <input type="hidden" th:field="*{id}" th:if="${product.id != null}">
                            
                            <!-- Name Field -->
                            <div class="mb-3">
                                <label for="name" class="form-label">Product Name</label>
                                <input type="text" 
                                       class="form-control" 
                                       id="name" 
                                       th:field="*{name}"
                                       placeholder="Enter product name"
                                       required>
                                <div class="invalid-feedback" th:errors="*{name}">
                                    Please enter a product name
                                </div>
                                <div class="form-text">Minimum 3 characters</div>
                            </div>
                            
                            <!-- Category Select -->
                            <div class="mb-3">
                                <label for="category" class="form-label">Category</label>
                                <select class="form-select" 
                                        id="category" 
                                        th:field="*{categoryId}"
                                        required>
                                    <option value="">Select category</option>
                                    <option th:each="cat : ${categories}"
                                            th:value="${cat.id}"
                                            th:text="${cat.name}"
                                            th:selected="${cat.id == product.categoryId}">
                                    </option>
                                </select>
                                <div class="invalid-feedback" th:errors="*{categoryId}">
                                    Please select a category
                                </div>
                            </div>
                            
                            <!-- Price Field -->
                            <div class="mb-3">
                                <label for="price" class="form-label">Price (USD)</label>
                                <div class="input-group">
                                    <span class="input-group-text">$</span>
                                    <input type="number" 
                                           class="form-control" 
                                           id="price" 
                                           th:field="*{price}"
                                           step="0.01"
                                           min="0.01"
                                           placeholder="0.00"
                                           required>
                                    <div class="invalid-feedback" th:errors="*{price}">
                                        Please enter a valid price
                                    </div>
                                </div>
                            </div>
                            
                            <!-- Description Field -->
                            <div class="mb-3">
                                <label for="description" class="form-label">Description</label>
                                <textarea class="form-control" 
                                          id="description" 
                                          th:field="*{description}"
                                          rows="4"
                                          placeholder="Enter product description"></textarea>
                            </div>
                            
                            <!-- Checkbox -->
                            <div class="mb-3 form-check">
                                <input type="checkbox" 
                                       class="form-check-input" 
                                       id="inStock" 
                                       th:field="*{inStock}">
                                <label class="form-check-label" for="inStock">
                                    In Stock
                                </label>
                            </div>
                            
                            <!-- Submit Buttons -->
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary">
                                    <i class="bi bi-save me-1"></i>
                                    <span th:text="${product.id != null ? 'Update' : 'Create'}">Save</span>
                                </button>
                                <a th:href="@{/admin/products}" class="btn btn-secondary">
                                    <i class="bi bi-x-circle me-1"></i>
                                    Cancel
                                </a>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</body>
</html>
```

JavaScript Organization

main.js - Global JavaScript
```js
// main.js - Global application JavaScript
(function() {
    'use strict';
    
    // Application namespace
    const App = {
        config: {
            apiBaseUrl: window.location.origin + '/api',
            csrfToken: document.querySelector('meta[name="_csrf"]')?.content,
            csrfHeader: document.querySelector('meta[name="_csrf_header"]')?.content
        },
        
        init() {
            this.initAutoDismissAlerts();
            this.initConfirmDialogs();
            this.initFormValidation();
            this.initTooltips();
            this.initPopovers();
        },
        
        // Auto-dismiss alerts after 5 seconds
        initAutoDismissAlerts() {
            document.querySelectorAll('.alert-dismissible').forEach(alert => {
                setTimeout(() => {
                    const closeBtn = alert.querySelector('.btn-close');
                    if (closeBtn) closeBtn.click();
                }, 5000);
            });
        },
        
        // Confirm dialogs for delete actions
        initConfirmDialogs() {
            document.querySelectorAll('[data-confirm]').forEach(element => {
                element.addEventListener('click', (e) => {
                    if (!confirm(element.dataset.confirm || 'Are you sure?')) {
                        e.preventDefault();
                    }
                });
            });
        },
        
        // Bootstrap form validation
        initFormValidation() {
            document.querySelectorAll('.needs-validation').forEach(form => {
                form.addEventListener('submit', (event) => {
                    if (!form.checkValidity()) {
                        event.preventDefault();
                        event.stopPropagation();
                    }
                    form.classList.add('was-validated');
                });
            });
        },
        
        // Initialize tooltips
        initTooltips() {
            document.querySelectorAll('[data-bs-toggle="tooltip"]')
                .forEach(el => new bootstrap.Tooltip(el));
        },
        
        // Initialize popovers
        initPopovers() {
            document.querySelectorAll('[data-bs-toggle="popover"]')
                .forEach(el => new bootstrap.Popover(el));
        },
        
        // Helper: format currency
        formatCurrency(amount) {
            return new Intl.NumberFormat('en-US', {
                style: 'currency',
                currency: 'USD'
            }).format(amount);
        },
        
        // Helper: make AJAX request
        async fetch(url, options = {}) {
            const defaultOptions = {
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            };
            
            if (this.config.csrfToken && this.config.csrfHeader) {
                defaultOptions.headers[this.config.csrfHeader] = this.config.csrfToken;
            }
            
            const response = await fetch(url, { ...defaultOptions, ...options });
            if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);
            return response.json();
        }
    };
    
    // Initialize when DOM is ready
    document.addEventListener('DOMContentLoaded', () => App.init());
    
    // Expose App globally
    window.App = App;
})();
```

components/forms.js
```js
// components/forms.js - Form-specific functionality
(function() {
    'use strict';
    
    class FormHandler {
        constructor(formSelector) {
            this.form = document.querySelector(formSelector);
            if (!this.form) return;
            
            this.init();
        }
        
        init() {
            this.setupAutoSubmit();
            this.setupDependentFields();
            this.setupCharacterCounter();
            this.setupImagePreview();
        }
        
        setupAutoSubmit() {
            this.form.querySelectorAll('[data-auto-submit]').forEach(select => {
                select.addEventListener('change', () => this.form.submit());
            });
        }
        
        setupDependentFields() {
            // Show/hide fields based on selections
            const triggers = this.form.querySelectorAll('[data-toggle-field]');
            triggers.forEach(trigger => {
                trigger.addEventListener('change', (e) => {
                    const targetId = trigger.dataset.toggleField;
                    const target = document.getElementById(targetId);
                    if (target) {
                        const showValue = trigger.dataset.showValue || 'true';
                        target.closest('.mb-3').style.display = 
                            e.target.value === showValue ? 'block' : 'none';
                    }
                });
                // Initial state
                trigger.dispatchEvent(new Event('change'));
            });
        }
        
        setupCharacterCounter() {
            this.form.querySelectorAll('[data-max-length]').forEach(field => {
                const maxLength = parseInt(field.dataset.maxLength);
                const counter = document.createElement('small');
                counter.className = 'text-muted float-end';
                field.parentNode.appendChild(counter);
                
                field.addEventListener('input', () => {
                    const remaining = maxLength - field.value.length;
                    counter.textContent = `${remaining} characters remaining`;
                    counter.style.color = remaining < 10 ? '#dc3545' : '#6c757d';
                });
                
                field.dispatchEvent(new Event('input'));
            });
        }
        
        setupImagePreview() {
            this.form.querySelectorAll('[data-image-preview]').forEach(input => {
                const previewId = input.dataset.imagePreview;
                const preview = document.getElementById(previewId);
                
                if (!preview) return;
                
                input.addEventListener('change', (e) => {
                    const file = e.target.files[0];
                    if (!file) return;
                    
                    const reader = new FileReader();
                    reader.onload = (event) => {
                        preview.src = event.target.result;
                        preview.style.display = 'block';
                    };
                    reader.readAsDataURL(file);
                });
            });
        }
    }
    
    // Initialize form handlers
    document.addEventListener('DOMContentLoaded', () => {
        new FormHandler('.needs-validation');
        new FormHandler('.ajax-form');
    });
})();
```

pages/dashboard.js
```js
// pages/dashboard.js - Dashboard page specific functionality
(function() {
    'use strict';
    
    class Dashboard {
        constructor() {
            this.charts = [];
            this.refreshInterval = null;
            this.init();
        }
        
        init() {
            this.initCharts();
            this.initRealTimeUpdates();
            this.initDataTable();
        }
        
        initCharts() {
            // Chart.js or other library integration
            const ctx = document.getElementById('salesChart');
            if (!ctx) return;
            
            // Example chart configuration
            this.charts.push(new Chart(ctx, {
                type: 'line',
                data: {
                    labels: ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun'],
                    datasets: [{
                        label: 'Sales',
                        data: [12, 19, 3, 5, 2, 3],
                        borderColor: '#0d6efd',
                        backgroundColor: 'rgba(13, 110, 253, 0.1)',
                        tension: 0.4
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: {
                            position: 'top'
                        }
                    }
                }
            }));
        }
        
        initRealTimeUpdates() {
            // WebSocket or polling for real-time updates
            const statsContainer = document.querySelector('[data-realtime-stats]');
            if (!statsContainer) return;
            
            this.refreshInterval = setInterval(() => {
                this.fetchStats();
            }, 30000); // Every 30 seconds
        }
        
        async fetchStats() {
            try {
                const stats = await window.App.fetch('/api/dashboard/stats');
                this.updateStats(stats);
            } catch (error) {
                console.error('Failed to fetch stats:', error);
            }
        }
        
        updateStats(stats) {
            // Update DOM with new stats
            document.querySelector('[data-stat-orders]').textContent = stats.orders;
            document.querySelector('[data-stat-revenue]').textContent = 
                window.App.formatCurrency(stats.revenue);
            document.querySelector('[data-stat-customers]').textContent = stats.customers;
        }
        
        initDataTable() {
            // DataTable initialization
            const table = document.querySelector('[data-datatable]');
            if (!table) return;
            
            // Simple table search and sort
            const searchInput = document.querySelector('[data-table-search]');
            if (searchInput) {
                searchInput.addEventListener('keyup', (e) => {
                    const term = e.target.value.toLowerCase();
                    table.querySelectorAll('tbody tr').forEach(row => {
                        const text = row.textContent.toLowerCase();
                        row.style.display = text.includes(term) ? '' : 'none';
                    });
                });
            }
        }
        
        destroy() {
            if (this.refreshInterval) {
                clearInterval(this.refreshInterval);
            }
            this.charts.forEach(chart => chart.destroy());
        }
    }
    
    // Initialize dashboard when DOM is ready
    const dashboard = new Dashboard();
    
    // Clean up on page unload
    window.addEventListener('beforeunload', () => dashboard.destroy());
})();
```

Bootstrap 5 Component Usage

Navbar Fragment
```html
<!-- fragments/navbar.html -->
<nav class="navbar navbar-expand-lg navbar-dark bg-primary shadow" 
     th:fragment="navbar">
    <div class="container">
        <a class="navbar-brand" th:href="@{/}">
            <i class="bi bi-shop me-2"></i>
            <span th:text="${appName}">My Store</span>
        </a>
        
        <button class="navbar-toggler" type="button" 
                data-bs-toggle="collapse" 
                data-bs-target="#navbarMain"
                aria-controls="navbarMain" 
                aria-expanded="false" 
                aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
        </button>
        
        <div class="collapse navbar-collapse" id="navbarMain">
            <ul class="navbar-nav me-auto mb-2 mb-lg-0">
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/}" 
                       th:classappend="${#httpServletRequest.requestURI == '/' ? 'active' : ''}">
                        <i class="bi bi-house"></i> Home
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link" th:href="@{/products}"
                       th:classappend="${#httpServletRequest.requestURI.contains('/products') ? 'active' : ''}">
                        <i class="bi bi-box"></i> Products
                    </a>
                </li>
                <li class="nav-item dropdown" 
                    sec:authorize="hasRole('ROLE_ADMIN')">
                    <a class="nav-link dropdown-toggle" 
                       href="#" 
                       id="adminDropdown"
                       role="button" 
                       data-bs-toggle="dropdown" 
                       aria-expanded="false">
                        <i class="bi bi-gear"></i> Admin
                    </a>
                    <ul class="dropdown-menu" aria-labelledby="adminDropdown">
                        <li><a class="dropdown-item" th:href="@{/admin/products}">
                            <i class="bi bi-box-seam"></i> Products
                        </a></li>
                        <li><a class="dropdown-item" th:href="@{/admin/orders}">
                            <i class="bi bi-cart"></i> Orders
                        </a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li><a class="dropdown-item" th:href="@{/admin/users}">
                            <i class="bi bi-people"></i> Users
                        </a></li>
                    </ul>
                </li>
            </ul>
            
            <!-- Search Form -->
            <form class="d-flex me-3" th:action="@{/search}" method="get">
                <div class="input-group">
                    <input class="form-control" type="search" 
                           name="q" placeholder="Search..."
                           aria-label="Search">
                    <button class="btn btn-light" type="submit">
                        <i class="bi bi-search"></i>
                    </button>
                </div>
            </form>
            
            <!-- User Menu -->
            <ul class="navbar-nav">
                <li class="nav-item dropdown">
                    <a class="nav-link dropdown-toggle" 
                       href="#" 
                       id="userDropdown"
                       role="button" 
                       data-bs-toggle="dropdown" 
                       aria-expanded="false"
                       sec:authorize="isAuthenticated()">
                        <i class="bi bi-person-circle"></i>
                        <span sec:authentication="name">User</span>
                    </a>
                    <ul class="dropdown-menu dropdown-menu-end" 
                        aria-labelledby="userDropdown">
                        <li><a class="dropdown-item" th:href="@{/profile}">
                            <i class="bi bi-person"></i> Profile
                        </a></li>
                        <li><a class="dropdown-item" th:href="@{/orders}">
                            <i class="bi bi-list-ul"></i> My Orders
                        </a></li>
                        <li><hr class="dropdown-divider"></li>
                        <li>
                            <form th:action="@{/logout}" method="post">
                                <button class="dropdown-item" type="submit">
                                    <i class="bi bi-box-arrow-right"></i> Logout
                                </button>
                            </form>
                        </li>
                    </ul>
                </li>
                <li class="nav-item" sec:authorize="!isAuthenticated()">
                    <a class="nav-link" th:href="@{/login}">
                        <i class="bi bi-box-arrow-in-right"></i> Login
                    </a>
                </li>
            </ul>
        </div>
    </div>
</nav>
```

Alerts Fragment
```html
<!-- fragments/alerts.html -->
<div th:fragment="alerts" class="mt-3">
    <!-- Success Alert -->
    <div th:if="${success != null}" 
         class="alert alert-success alert-dismissible fade show" 
         role="alert">
        <i class="bi bi-check-circle me-2"></i>
        <span th:text="${success}">Success message</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" 
                aria-label="Close"></button>
    </div>
    
    <!-- Error Alert -->
    <div th:if="${error != null}" 
         class="alert alert-danger alert-dismissible fade show" 
         role="alert">
        <i class="bi bi-exclamation-circle me-2"></i>
        <span th:text="${error}">Error message</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" 
                aria-label="Close"></button>
    </div>
    
    <!-- Warning Alert -->
    <div th:if="${warning != null}" 
         class="alert alert-warning alert-dismissible fade show" 
         role="alert">
        <i class="bi bi-exclamation-triangle me-2"></i>
        <span th:text="${warning}">Warning message</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" 
                aria-label="Close"></button>
    </div>
    
    <!-- Info Alert -->
    <div th:if="${info != null}" 
         class="alert alert-info alert-dismissible fade show" 
         role="alert">
        <i class="bi bi-info-circle me-2"></i>
        <span th:text="${info}">Info message</span>
        <button type="button" class="btn-close" data-bs-dismiss="alert" 
                aria-label="Close"></button>
    </div>
</div>
```

Custom CSS

custom.css
```css
/* custom.css - Global custom styles */

/* Variables */
:root {
    --primary-color: #0d6efd;
    --secondary-color: #6c757d;
    --success-color: #198754;
    --danger-color: #dc3545;
    --warning-color: #ffc107;
    --info-color: #0dcaf0;
    --light-color: #f8f9fa;
    --dark-color: #212529;
}

/* Typography */
body {
    min-height: 100vh;
    display: flex;
    flex-direction: column;
}

main {
    flex: 1;
}

/* Cards */
.card {
    border-radius: 12px;
    border: none;
    box-shadow: 0 2px 4px rgba(0,0,0,0.08);
    transition: transform 0.2s, box-shadow 0.2s;
}

.card:hover {
    box-shadow: 0 4px 8px rgba(0,0,0,0.12);
}

.card-header {
    border-radius: 12px 12px 0 0 !important;
}

/* Images */
.img-fluid {
    max-height: 400px;
    object-fit: cover;
}

.rounded-circle {
    border: 2px solid #fff;
    box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

/* Animations */
.fade-in {
    animation: fadeIn 0.5s ease-in-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}

/* Spacing utilities */
.py-6 {
    padding-top: 4rem !important;
    padding-bottom: 4rem !important;
}

.mt-6 {
    margin-top: 4rem !important;
}

/* Buttons */
.btn {
    border-radius: 8px;
    padding: 0.5rem 1.5rem;
    font-weight: 500;
    transition: all 0.2s;
}

.btn-primary {
    background: linear-gradient(135deg, #0d6efd, #0a58ca);
    border: none;
}

.btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(13, 110, 253, 0.3);
}

/* Tables */
.table {
    border-radius: 12px;
    overflow: hidden;
}

.table thead {
    background: var(--light-color);
}

/* Badges */
.badge {
    padding: 0.5rem 1rem;
    border-radius: 50px;
    font-weight: 500;
}

/* Forms */
.form-control, .form-select {
    border-radius: 8px;
    border: 1px solid #dee2e6;
    padding: 0.6rem 1rem;
}

.form-control:focus, .form-select:focus {
    border-color: var(--primary-color);
    box-shadow: 0 0 0 0.2rem rgba(13, 110, 253, 0.25);
}

.form-label {
    font-weight: 500;
    margin-bottom: 0.3rem;
}

/* Navbar */
.nav
```

