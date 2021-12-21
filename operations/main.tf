provider "aws" {
  region = "us-west-2" # Oregon
}

resource "aws_s3_bucket" "project" {
  bucket = "david.jdbernard.com"
  acl    = "log-delivery-write"
}

data "aws_iam_policy_document" "bucket_access_policy" {
  statement {
    actions   = [ "s3:GetObject" ]
    effect    = "Allow"
    resources = [ "${aws_s3_bucket.project.arn}/webroot/*" ]

    principals {
      type        = "AWS"
      identifiers = [ aws_cloudfront_origin_access_identity.origin_access_identity.iam_arn ]
    }
  }

  statement {
    actions   = [ "s3:ListBucket" ]
    effect    = "Allow"
    resources = [ aws_s3_bucket.project.arn ]

    principals {
      type        = "AWS"
      identifiers = [ aws_cloudfront_origin_access_identity.origin_access_identity.iam_arn ]
    }
  }
}

output "oai_access_policy" {
 value = data.aws_iam_policy_document.bucket_access_policy
}

resource "aws_cloudfront_origin_access_identity" "origin_access_identity" {
  comment = "OAI for DKB Photo Site."
}

resource "aws_cloudfront_distribution" "s3_distribution" {
  origin {
    domain_name = aws_s3_bucket.project.bucket_regional_domain_name
    origin_id   = "S3-david.jdbernard.com"
    origin_path = "/webroot"

    s3_origin_config {
      origin_access_identity = aws_cloudfront_origin_access_identity.origin_access_identity.cloudfront_access_identity_path
    }
  }

  enabled             = true
  is_ipv6_enabled     = true
  comment             = "DKB Photo Site distribution."
  default_root_object = "/index.html"

  logging_config {
    include_cookies = false
    bucket          = aws_s3_bucket.project.bucket_domain_name
    prefix          = "logs/cloudfront"
  }

  aliases = ["david.jdbernard.com"]

  default_cache_behavior {
    allowed_methods   = ["GET", "HEAD", "OPTIONS"]
    cached_methods    = ["GET", "HEAD", "OPTIONS"]
    target_origin_id  = "S3-david.jdbernard.com"

    forwarded_values {
      query_string = false

      cookies {
        forward = "none"
      }
    }

    min_ttl                 = 0
    default_ttl             = 60 * 60 * 24 * 365  # cache for a year
    max_ttl                 = 60 * 60 * 24 * 365  # cache for a year
    compress                = true
    viewer_protocol_policy  = "redirect-to-https"
  }

  price_class = "PriceClass_100" # US and Canada only

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn = "arn:aws:acm:us-east-1:063932952339:certificate/7f5a1a5f-25f0-4ccb-998e-f4908a7ec9a9"
    ssl_support_method  = "sni-only"
  }
}
