{
  description = "A webapp to split cost on a pay-what-you-can basis";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    clj-nix.url = "github:jlesquembre/clj-nix";
  };

  outputs = {
    self,
    nixpkgs,
    flake-utils,
    clj-nix,
  }:
    flake-utils.lib.eachDefaultSystem (system: let
      pkgs = nixpkgs.legacyPackages.${system};
    in {
      packages = {
        default = clj-nix.lib.mkCljApp {
          inherit pkgs;
          modules = [
            {
              projectSrc = ./.;
              name = "net.sohalt/fairsplit";
              main-ns = "net.sohalt.fairsplit.main";
              nativeImage = {
                enable = true;
                graalvm = pkgs.graalvmCEPackages.graalvm-ce;
              };
            }
          ];
        };
      };

      devShells.default = pkgs.mkShell {
        packages = with pkgs; [
          clojure
          babashka
          clj-nix.packages.${system}.deps-lock
        ];
      };
    });
}
