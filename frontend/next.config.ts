import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  async redirects() {
    return [
      { source: "/zgloszenia", destination: "/operator", permanent: false },
      { source: "/budynki", destination: "/analityk/budynki", permanent: false },
      { source: "/prognoza", destination: "/analityk/prognoza", permanent: false },
      { source: "/plan", destination: "/analityk/plan", permanent: false },
    ];
  },
  async rewrites() {
    return [
      { source: "/operator", destination: "/zgloszenia" },
      { source: "/analityk/budynki", destination: "/budynki" },
      { source: "/analityk/prognoza", destination: "/prognoza" },
      { source: "/analityk/plan", destination: "/plan" },
    ];
  },
};

export default nextConfig;
