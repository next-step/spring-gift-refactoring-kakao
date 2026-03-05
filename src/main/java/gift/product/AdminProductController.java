package gift.product;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/products")
public class AdminProductController {
    private final ProductService productService;

    public AdminProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("products", productService.findAll());
        return "product/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categories", productService.findAllCategories());
        return "product/new";
    }

    @PostMapping
    public String create(
        @RequestParam String name,
        @RequestParam int price,
        @RequestParam String imageUrl,
        @RequestParam Long categoryId,
        Model model
    ) {
        try {
            productService.createFromAdmin(name, price, imageUrl, categoryId);
        } catch (IllegalArgumentException e) {
            populateNewFormError(model, name, price, imageUrl, categoryId, e.getMessage());
            return "product/new";
        }
        return "redirect:/admin/products";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        var product = productService.findById(id);
        model.addAttribute("product", product);
        model.addAttribute("categories", productService.findAllCategories());
        return "product/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(
        @PathVariable Long id,
        @RequestParam String name,
        @RequestParam int price,
        @RequestParam String imageUrl,
        @RequestParam Long categoryId,
        Model model
    ) {
        try {
            productService.updateFromAdmin(id, name, price, imageUrl, categoryId);
        } catch (IllegalArgumentException e) {
            var product = productService.findById(id);
            populateEditFormError(model, product, name, price, imageUrl, categoryId, e.getMessage());
            return "product/edit";
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/products";
    }

    private void populateNewFormError(
        Model model,
        String name,
        int price,
        String imageUrl,
        Long categoryId,
        String error
    ) {
        model.addAttribute("error", error);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", productService.findAllCategories());
    }

    private void populateEditFormError(
        Model model,
        Product product,
        String name,
        int price,
        String imageUrl,
        Long categoryId,
        String error
    ) {
        model.addAttribute("error", error);
        model.addAttribute("product", product);
        model.addAttribute("name", name);
        model.addAttribute("price", price);
        model.addAttribute("imageUrl", imageUrl);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("categories", productService.findAllCategories());
    }
}
