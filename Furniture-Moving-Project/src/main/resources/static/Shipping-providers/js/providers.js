$(document).ready(function () {
  // Add active class to the shipping providers nav item
  $(".nav-item.nav-link").removeClass("active");
  $('a[href="/shipping-providers"]').addClass("active");

  // Initialize rating stars
  initRatingStars();

  // Initialize search/filter functionality
  initSearch();

  // Initialize sorting functionality
  initSorting();
});

function initRatingStars() {
  // Convert numeric ratings to star display
  $(".provider-rating").each(function () {
    const rating = parseFloat($(this).find("span").text());
    const starsHtml = getStarsHtml(rating);
    $(this).html(starsHtml + " <span>(" + rating.toFixed(1) + ")</span>");
  });
}

function getStarsHtml(rating) {
  let html = "";
  const fullStars = Math.floor(rating);
  const halfStar = rating % 1 >= 0.5;
  const emptyStars = 5 - fullStars - (halfStar ? 1 : 0);

  // Add full stars
  for (let i = 0; i < fullStars; i++) {
    html += '<i class="fa fa-star"></i>';
  }

  // Add half star if needed
  if (halfStar) {
    html += '<i class="fa fa-star-half-alt"></i>';
  }

  // Add empty stars
  for (let i = 0; i < emptyStars; i++) {
    html += '<i class="far fa-star"></i>';
  }

  return html;
}

function initSearch() {
  // Add search input to the page
  const searchHtml = `
        <div class="row mb-4">
            <div class="col-md-6 mx-auto">
                <div class="input-group">
                    <input type="text" id="provider-search" class="form-control" placeholder="Tìm kiếm đơn vị vận chuyển...">
                    <div class="input-group-append">
                        <button class="btn btn-primary" type="button">
                            <i class="fa fa-search"></i>
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;

  $(".section-header").after(searchHtml);

  // Handle search functionality
  $("#provider-search").on("keyup", function () {
    const searchTerm = $(this).val().toLowerCase();

    $(".provider-card").each(function () {
      const providerName = $(this).find(".provider-name").text().toLowerCase();
      if (providerName.includes(searchTerm)) {
        $(this).parent().show();
      } else {
        $(this).parent().hide();
      }
    });
  });
}

function initSorting() {
  // Add sorting options
  const sortingHtml = `
        <div class="row mb-4">
            <div class="col-md-6 mx-auto">
                <div class="form-group">
                    <label for="sort-providers">Sắp xếp theo:</label>
                    <select id="sort-providers" class="form-control">
                        <option value="name-asc">Tên (A-Z)</option>
                        <option value="name-desc">Tên (Z-A)</option>
                        <option value="rating-desc" selected>Đánh giá (Cao-Thấp)</option>
                        <option value="rating-asc">Đánh giá (Thấp-Cao)</option>
                        <option value="reviews-desc">Số lượng đánh giá (Cao-Thấp)</option>
                    </select>
                </div>
            </div>
        </div>
    `;

  $("#provider-search").closest(".row").after(sortingHtml);

  // Handle sorting
  $("#sort-providers").on("change", function () {
    const sortValue = $(this).val();
    const $providers = $(".col-lg-4.col-md-6.mb-4").get();

    $providers.sort(function (a, b) {
      switch (sortValue) {
        case "name-asc":
          return $(a)
            .find(".provider-name")
            .text()
            .localeCompare($(b).find(".provider-name").text());
        case "name-desc":
          return $(b)
            .find(".provider-name")
            .text()
            .localeCompare($(a).find(".provider-name").text());
        case "rating-desc":
          return (
            parseFloat($(b).find(".provider-rating span").text()) -
            parseFloat($(a).find(".provider-rating span").text())
          );
        case "rating-asc":
          return (
            parseFloat($(a).find(".provider-rating span").text()) -
            parseFloat($(b).find(".provider-rating span").text())
          );
        case "reviews-desc":
          return (
            parseInt($(b).find(".provider-reviews span").text()) -
            parseInt($(a).find(".provider-reviews span").text())
          );
        default:
          return 0;
      }
    });

    const $parent = $(".col-lg-4.col-md-6.mb-4").parent();
    $.each($providers, function (i, item) {
      $parent.append(item);
    });
  });
}
