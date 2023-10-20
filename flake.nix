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
      pkgs = import nixpkgs {
        inherit system;
        overlays = [self.overlays.default];
      };
    in {
      packages = {
        default = pkgs.solisplit;
        solisplit = pkgs.solisplit;
      };

      overlays.default = final: prev: {
        solisplit = clj-nix.lib.mkCljApp {
          pkgs = final;
          modules = [
            {
              projectSrc = ./.;
              name = "net.sohalt/solisplit";
              main-ns = "net.sohalt.solisplit.main";
              java-opts = ["-Dversion=\"${self.shortRev or "dev"}\""];
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
