{
  description = "An example deployment of solisplit using caddy as a reverse proxy";
  inputs.solisplit.url = "github:sohalt/solisplit";
  outputs = {
    nixpkgs,
    solisplit,
    ...
  }: {
    nixosConfigurations.example = nixpkgs.lib.nixosSystem {
      modules = [
        ({config, ...}: {
          imports = [solisplit.nixosModules.solisplit];
          nixpkgs.overlays = [solisplit.overlays.default];
          services.solisplit.enable = true;
          services.caddy = {
            enable = true;
            virtualHosts = {
              "solisplit.example.com" = {
                extraConfig = "reverse_proxy 127.0.0.1:${toString config.services.solisplit.port}";
              };
            };
          };
        })
      ];
    };
  };
}
